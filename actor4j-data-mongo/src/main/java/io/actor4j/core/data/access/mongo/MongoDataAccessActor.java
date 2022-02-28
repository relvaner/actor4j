/*
 * Copyright (c) 2015-2018, David A. Bauer. All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.actor4j.core.data.access.mongo;

import com.mongodb.MongoClient;

import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.data.access.DataAccessActor;
import io.actor4j.core.data.access.PersistentDataAccessObject;

import static io.actor4j.core.actors.ActorWithCache.*;
import static io.actor4j.core.data.access.mongo.MongoOperations.*;

import java.util.HashMap;
import java.util.Map;

import org.bson.Document;

public class MongoDataAccessActor<K, V> extends DataAccessActor<K, V> {
	protected MongoClient client;
	protected String databaseName;
	protected boolean bulkWrite;
	protected boolean bulkOrdered;
	protected int bulkSize;
	protected Class<V> valueType;
	protected Map<String, MongoBufferedBulkWriter> bulkWriters;
	
	public MongoDataAccessActor(String name, MongoClient client, String databaseName, boolean bulkWrite, boolean bulkOrdered, int bulkSize, Class<V> valueType) {
		super(name);
		this.client = client;
		this.databaseName = databaseName;
		this.bulkWrite = bulkWrite;
		this.bulkOrdered = bulkOrdered;
		this.bulkSize = bulkSize;
		this.valueType = valueType;
		
		bulkWriters = new HashMap<>();
	}
	
	public MongoDataAccessActor(MongoClient client, String databaseName, boolean bulkWrite, boolean bulkOrdered, int bulkSize, Class<V> valueType) {
		this(null, client, databaseName, bulkWrite, bulkOrdered, bulkSize, valueType);
	}
	
	public MongoDataAccessActor(String name, MongoClient client, String databaseName, Class<V> valueType) {
		this(name, client, databaseName, false, true, 0, valueType);
	}
	
	public MongoDataAccessActor(MongoClient client, String databaseName, Class<V> valueType) {
		this(null, client, databaseName, valueType);
	}

	@Override
	public void receive(ActorMessage<?> message) {
		if (message.value()!=null && message.value() instanceof PersistentDataAccessObject) {
			@SuppressWarnings("unchecked")
			PersistentDataAccessObject<K,V> obj = (PersistentDataAccessObject<K,V>)message.value();
			
			MongoBufferedBulkWriter bulkWriter = null;
			if (bulkWrite) {
				bulkWriter = bulkWriters.get(obj.collectionName);
				if (bulkWriter==null) {
					bulkWriter = new MongoBufferedBulkWriterImpl(client.getDatabase(databaseName).getCollection(obj.collectionName), bulkOrdered, bulkSize);
					bulkWriters.put(obj.collectionName, bulkWriter);
				}
			}
			
			if (message.tag()==FIND_ONE || message.tag()==GET) {
				obj.value = findOne(Document.parse(obj.filter), client, databaseName, obj.collectionName, valueType);
				tell(obj, FIND_ONE, message.source(), message.interaction());
			}
			else if (message.tag()==SET) {
				if (!((boolean)obj.reserved) && !hasOne(Document.parse(obj.filter), client, databaseName, obj.collectionName))
					insertOne(obj.value, client, databaseName, obj.collectionName, bulkWriter);
				else
					replaceOne(Document.parse(obj.filter), obj.value, client, databaseName, obj.collectionName, bulkWriter);
			}
			else if (message.tag()==UPDATE_ONE || message.tag()==UPDATE)
				updateOne(Document.parse(obj.filter), Document.parse(obj.update), client, databaseName, obj.collectionName, bulkWriter);
			else if (message.tag()==INSERT_ONE) {
				if (obj.filter!=null) {
					if (!hasOne(Document.parse(obj.filter), client, databaseName, obj.collectionName))
						insertOne(obj.value, client, databaseName, obj.collectionName, bulkWriter);
				}
				else
					insertOne(obj.value, client, databaseName, obj.collectionName, bulkWriter);
			}
			else if (message.tag()==DELETE_ONE)
				deleteOne(Document.parse(obj.filter), client, databaseName, obj.collectionName, bulkWriter);
			else if (message.tag()==HAS_ONE) {
				obj.reserved = hasOne(Document.parse(obj.filter), client, databaseName, obj.collectionName);
				tell(obj, FIND_ONE, message.source(), message.interaction());
			}
			else if (message.tag()==FLUSH && bulkWrite)
				bulkWriter.flush();
			else
				unhandled(message);
		}
		else
			unhandled(message);
	}
}
