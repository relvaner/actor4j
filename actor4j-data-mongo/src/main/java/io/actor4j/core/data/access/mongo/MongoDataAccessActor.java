/*
 * Copyright (c) 2015-2023, David A. Bauer. All rights reserved.
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

import com.mongodb.client.MongoClient;

import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.mongo.MongoBufferedBulkWriter;
import io.actor4j.core.data.access.DataAccessActor;
import io.actor4j.core.data.access.PersistentDataAccessDTO;

import static io.actor4j.core.actors.ActorWithCache.*;
import static io.actor4j.core.mongo.MongoOperations.*;

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
		if (message.value()!=null && message.value() instanceof PersistentDataAccessDTO) {
			@SuppressWarnings("unchecked")
			PersistentDataAccessDTO<K,V> dto = (PersistentDataAccessDTO<K,V>)message.value();
			
			try {
				MongoBufferedBulkWriter bulkWriter = null;
				if (bulkWrite) {
					bulkWriter = bulkWriters.get(dto.collectionName());
					if (bulkWriter==null) {
						bulkWriter = MongoBufferedBulkWriter.create(client, databaseName, dto.collectionName(), bulkOrdered, bulkSize);
						bulkWriters.put(dto.collectionName(), bulkWriter);
					}
				}
				
				if (message.tag()==FIND_ONE || message.tag()==GET) {
					V value = convertToValue(findOne(Document.parse(dto.filter()), client, databaseName, dto.collectionName()), valueType);
					tell(dto.shallowCopy(value), FIND_ONE, message.source(), message.interaction());
				}
				else if (message.tag()==SET) {
					if (!((boolean)dto.reserved()) && !hasOne(Document.parse(dto.filter()), client, databaseName, dto.collectionName()))
						insertOne(convertToDocument(dto.value()), client, databaseName, dto.collectionName(), bulkWriter);
					else
						replaceOne(Document.parse(dto.filter()), convertToDocument(dto.value()), client, databaseName, dto.collectionName(), bulkWriter);
				}
				else if (message.tag()==UPDATE_ONE || message.tag()==UPDATE)
					updateOne(Document.parse(dto.filter()), Document.parse(dto.update()), client, databaseName, dto.collectionName(), bulkWriter);
				else if (message.tag()==INSERT_ONE) {
					if (dto.filter()!=null) {
						if (!hasOne(Document.parse(dto.filter()), client, databaseName, dto.collectionName()))
							insertOne(convertToDocument(dto.value()), client, databaseName, dto.collectionName(), bulkWriter);
					}
					else
						insertOne(convertToDocument(dto.value()), client, databaseName, dto.collectionName(), bulkWriter);
				}
				else if (message.tag()==DELETE_ONE)
					deleteOne(Document.parse(dto.filter()), client, databaseName, dto.collectionName(), bulkWriter);
				else if (message.tag()==HAS_ONE) {
					Object reserved = hasOne(Document.parse(dto.filter()), client, databaseName, dto.collectionName());
					tell(dto.shallowCopyWithReserved(reserved), FIND_ONE, message.source(), message.interaction());
				}
				else if (message.tag()==FLUSH && bulkWrite)
					bulkWriter.flush();
				else
					unhandled(message);
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		else
			unhandled(message);
	}
}
