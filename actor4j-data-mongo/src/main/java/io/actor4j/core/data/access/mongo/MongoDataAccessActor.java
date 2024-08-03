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
import com.mongodb.client.model.WriteModel;

import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.CircuitBreaker;
import io.actor4j.core.utils.Pair;
import io.actor4j.database.mongo.MongoBufferedBulkWriter;
import io.actor4j.core.data.access.DataAccessActor;
import io.actor4j.core.data.access.PersistentFailureDTO;
import io.actor4j.core.data.access.PersistentSuccessDTO;
import io.actor4j.core.data.access.PersistentDataAccessDTO;

import static io.actor4j.core.actors.ActorWithCache.*;
import static io.actor4j.database.mongo.MongoOperations.*;
//import static io.actor4j.core.logging.ActorLogger.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bson.Document;

public class MongoDataAccessActor<K, V> extends DataAccessActor<K, V> {
	protected final MongoClient client;
	protected final String databaseName;
	protected final boolean bulkWrite;
	protected final boolean bulkOrdered;
	protected final int bulkSize;
	protected final Class<V> valueType;
	protected final Map<String, MongoBufferedBulkWriter> bulkWriters;
	protected final Map<UUID, Pair<UUID, PersistentDataAccessDTO<K, V>>> bulkWriterRequests; // interaction -> source
	protected final CircuitBreaker circuitBreaker;
	
	public MongoDataAccessActor(String name, MongoClient client, String databaseName, 
			boolean bulkWrite, boolean bulkOrdered, int bulkSize, Class<V> valueType, int maxFailures, long resetTimeout) {
		super(name, true); // @Stateful
		
		this.client = client;
		this.databaseName = databaseName;
		this.bulkWrite = bulkWrite;
		this.bulkOrdered = bulkOrdered;
		this.bulkSize = bulkSize;
		this.valueType = valueType;
		
		bulkWriters = new HashMap<>();
		bulkWriterRequests = new HashMap<>();
		circuitBreaker = new CircuitBreaker(maxFailures, resetTimeout);
	}
	
	public MongoDataAccessActor(MongoClient client, String databaseName, 
			boolean bulkWrite, boolean bulkOrdered, int bulkSize, Class<V> valueType, int maxFailures, long resetTimeout) {
		this(null, client, databaseName, bulkWrite, bulkOrdered, bulkSize, valueType, maxFailures, resetTimeout);
	}
	
	public MongoDataAccessActor(String name, MongoClient client, String databaseName, Class<V> valueType, int maxFailures, long resetTimeout) {
		this(name, client, databaseName, false, true, 0, valueType, maxFailures, resetTimeout);
	}
	
	public MongoDataAccessActor(String name, MongoClient client, String databaseName, Class<V> valueType) {
		this(name, client, databaseName, false, true, 0, valueType, DEFAULT_MAX_FAILURES, DEFAULT_RESET_TIMEOUT);
	}
	
	public MongoDataAccessActor(MongoClient client, String databaseName, Class<V> valueType, int maxFailures, long resetTimeout) {
		this(null, client, databaseName, valueType, maxFailures, resetTimeout);
	}
	
	public MongoDataAccessActor(MongoClient client, String databaseName, Class<V> valueType) {
		this(null, client, databaseName, valueType, DEFAULT_MAX_FAILURES, DEFAULT_RESET_TIMEOUT);
	}
	
	public void onBulkWriterSuccess(List<Pair<UUID, WriteModel<Document>>> requests) {
		for (Pair<UUID, WriteModel<Document>> pair : requests) {
			Pair<UUID, PersistentDataAccessDTO<K, V>> originRequest = bulkWriterRequests.get(pair.a());
			tell(PersistentSuccessDTO.of(originRequest.b(), 0), SUCCESS, originRequest.a(/*source*/), pair.a()/*interaction*/);
		}
	}
	
	public void onBulkWriterError(List<Pair<UUID, WriteModel<Document>>> requests, Throwable t) {
		for (Pair<UUID, WriteModel<Document>> pair : requests) {
			Pair<UUID, PersistentDataAccessDTO<K, V>> originRequest = bulkWriterRequests.get(pair.a());
			tell(PersistentFailureDTO.of(originRequest.b(), 0, t), FAILURE, originRequest.a(/*source*/), pair.a()/*interaction*/);
		}
	}

	@Override
	public void receive(ActorMessage<?> message) {
		if (message.value()!=null && message.value() instanceof PersistentDataAccessDTO) {
			@SuppressWarnings("unchecked")
			PersistentDataAccessDTO<K,V> dto = (PersistentDataAccessDTO<K,V>)message.value();
			
			if (circuitBreaker.isCallable()) {
				try {
					MongoBufferedBulkWriter bulkWriter = null;
					if (bulkWrite) {
						bulkWriter = bulkWriters.get(dto.collectionName());
						if (bulkWriter==null) {
							bulkWriter = MongoBufferedBulkWriter.create(client, databaseName, dto.collectionName(), bulkOrdered, bulkSize, 
								this::onBulkWriterSuccess, this::onBulkWriterError);
							bulkWriters.put(dto.collectionName(), bulkWriter);
						}
					}
					
					boolean unhandled = false;
					if (message.tag()==FIND_ONE || message.tag()==GET) {
						V value = convertToValue(findOne(Document.parse(dto.filter().encode()), client, databaseName, dto.collectionName()), valueType);
						if (dto.value()!=null)
							tell(dto.shallowCopy(value), FIND_ONE, message.source(), message.interaction());
						else
							tell(dto, FIND_NONE, message.source(), message.interaction());
					}
					else if (message.tag()==SET) {
						if (!((boolean)dto.reserved()) && !hasOne(Document.parse(dto.filter().encode()), client, databaseName, dto.collectionName()))
							insertOne(convertToDocument(dto.value()), message.interaction(), client, databaseName, dto.collectionName(), bulkWriter);
						else
							replaceOne(Document.parse(dto.filter().encode()), convertToDocument(dto.value()), message.interaction(), client, databaseName, dto.collectionName(), bulkWriter);
					}
					else if (message.tag()==UPDATE_ONE || message.tag()==UPDATE)
						updateOne(Document.parse(dto.filter().encode()), Document.parse(dto.update().encode()), message.interaction(), client, databaseName, dto.collectionName(), bulkWriter);
					else if (message.tag()==INSERT_ONE) {
						if (dto.filter()!=null) {
							if (!hasOne(Document.parse(dto.filter().encode()), client, databaseName, dto.collectionName()))
								insertOne(convertToDocument(dto.value()), message.interaction(), client, databaseName, dto.collectionName(), bulkWriter);
						}
						else
							insertOne(convertToDocument(dto.value()), message.interaction(), client, databaseName, dto.collectionName(), bulkWriter);
					}
					else if (message.tag()==DELETE_ONE)
						deleteOne(Document.parse(dto.filter().encode()), message.interaction(), client, databaseName, dto.collectionName(), bulkWriter);
					else if (message.tag()==HAS_ONE) {
						Object reserved = hasOne(Document.parse(dto.filter().encode()), client, databaseName, dto.collectionName());
						tell(dto.shallowCopyWithReserved(reserved), FIND_ONE, message.source(), message.interaction());
					}
					else if (message.tag()==FLUSH && bulkWrite)
						bulkWriter.flush();
					else {
						unhandled = true;
						unhandled(message);
					}
					
					if (!unhandled) {
						circuitBreaker.success();
						if (!bulkWrite && message.tag()!=FIND_ONE && message.tag()!=GET && message.tag()!=HAS_ONE)
							tell(PersistentSuccessDTO.of(dto, message.tag()), SUCCESS, message.source(), message.interaction());
					}
					else
						tell(dto, ActorMessage.UNHANDLED, message.source(), message.interaction());
				}
				catch(Exception e) { 
					// Inclusively invocation timeout regarding MongoClient, Retryable Reads/Writes
					// @See https://www.mongodb.com/docs/drivers/java/sync/v4.11/fundamentals/connection/mongoclientsettings/
					
					e.printStackTrace();
					
					circuitBreaker.failure();
					if (!bulkWrite)
						tell(PersistentFailureDTO.of(dto, message.tag(), e), FAILURE, message.source(), message.interaction());
				}
			}

//			systemLogger().log(DEBUG, "circuit breaker: "+circuitBreaker.getState());
		}
		else
			unhandled(message);
	}
}
