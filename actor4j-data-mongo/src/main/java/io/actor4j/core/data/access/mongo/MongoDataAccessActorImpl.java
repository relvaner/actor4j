/*
 * Copyright (c) 2015-2024, David A. Bauer. All rights reserved.
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

import static io.actor4j.core.data.access.DataAccessActor.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.model.WriteModel;

import io.actor4j.core.actors.ActorRef;
import io.actor4j.core.actors.ActorWithCache;
import io.actor4j.core.data.access.BaseDataAccessActorImpl;
import io.actor4j.core.data.access.PersistentDataAccessDTO;
import io.actor4j.core.data.access.PersistentFailureDTO;
import io.actor4j.core.data.access.PersistentSuccessDTO;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.Pair;
import io.actor4j.database.mongo.MongoBufferedBulkWriter;
import io.actor4j.database.mongo.MongoOperations;

import static io.actor4j.database.mongo.MongoOperations.*;

public class MongoDataAccessActorImpl<K, E> extends BaseDataAccessActorImpl<K, E> {
	protected record BulkWriterRequest<K, E>(int tag, UUID interaction, UUID source, PersistentDataAccessDTO<K, E> dto) {
	}
	
	protected final MongoClient client;
	protected final String databaseName;
	protected final boolean bulkWrite;
	protected final boolean bulkOrdered;
	protected final int bulkSize;
	protected final Class<E> entityType;
	protected final Map<String, MongoBufferedBulkWriter> bulkWriters;
	protected final Map<UUID, BulkWriterRequest<K, E>> bulkWriterRequests; // id -> request
	
	protected MongoBufferedBulkWriter selectedBulkWriter;
	
	public MongoDataAccessActorImpl(ActorRef dataAccess, MongoClient client, String databaseName, 
			boolean bulkWrite, boolean bulkOrdered, int bulkSize, Class<E> entityType, int maxFailures, long resetTimeout) {
		super(dataAccess);
		
		this.client = client;
		this.databaseName = databaseName;
		this.bulkWrite = bulkWrite;
		this.bulkOrdered = bulkOrdered;
		this.bulkSize = bulkSize;
		this.entityType = entityType;
		
		bulkWriters = new HashMap<>();
		bulkWriterRequests = new HashMap<>();
	}
	
	public void onBulkWriterSuccess(List<Pair<UUID, WriteModel<Document>>> requests) {
		for (Pair<UUID, WriteModel<Document>> pair : requests) {
			BulkWriterRequest<K, E> originRequest = bulkWriterRequests.get(pair.a()/*id*/);
			dataAccess.tell(PersistentSuccessDTO.of(originRequest.dto(), originRequest.tag()), SUCCESS, originRequest.source(), originRequest.interaction());
			
			bulkWriterRequests.remove(pair.a());
		}
	}
	
	public void onBulkWriterError(List<Pair<UUID, WriteModel<Document>>> requests, Throwable t) {
		for (Pair<UUID, WriteModel<Document>> pair : requests) {
			BulkWriterRequest<K, E> originRequest = bulkWriterRequests.get(pair.a()/*id*/);
			dataAccess.tell(PersistentFailureDTO.of(originRequest.dto(), originRequest.tag(), t), FAILURE, originRequest.source(), originRequest.interaction());
			
			bulkWriterRequests.remove(pair.a());
		}
	}

	@Override
	public void onReceiveMessage(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		if (bulkWrite) {
			MongoBufferedBulkWriter bulkWriter = bulkWriters.get(dto.collectionName());
			if (bulkWriter==null) {
				bulkWriter = MongoBufferedBulkWriter.create(client, databaseName, dto.collectionName(), bulkOrdered, bulkSize, 
					this::onBulkWriterSuccess, this::onBulkWriterError);
				bulkWriters.put(dto.collectionName(), bulkWriter);
			}
			
			selectedBulkWriter = bulkWriter;
			
			if (msg.tag()!=FIND_ONE && msg.tag()!=ActorWithCache.GET && msg.tag()!=HAS_ONE)
				bulkWriterRequests.put(dto.id(), new BulkWriterRequest<>(msg.tag(), msg.interaction(), msg.source(), dto));
		}
	}
	
	@Override
	public void queryOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void queryAll(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void findOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		E entity = convertToEntity(MongoOperations.findOne(Document.parse(dto.filter().encode()), client, databaseName, dto.collectionName()), entityType);
		if (entity!=null)
			dataAccess.tell(dto.shallowCopy(entity), FIND_ONE, msg.source(), msg.interaction());
		else
			dataAccess.tell(dto, FIND_NONE, msg.source(), msg.interaction());
	}
	
	@Override
	public void findAll(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		List<E> entities = convertToEntities(MongoOperations.findAll(Document.parse(dto.filter().encode()), client, databaseName, dto.collectionName()), entityType);
		if (entities!=null)
			dataAccess.tell(dto.shallowCopyWithEntities(entities), FIND_ALL, msg.source(), msg.interaction());
		else
			dataAccess.tell(dto, FIND_NONE, msg.source(), msg.interaction());
	}

	@Override
	public boolean hasOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		return MongoOperations.hasOne(Document.parse(dto.filter().encode()), client, databaseName, dto.collectionName());
	}

	@Override
	public void insertOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		MongoOperations.insertOne(convertToDocument(dto.value()), dto.id(), client, databaseName, dto.collectionName(), selectedBulkWriter);
	}

	@Override
	public void replaceOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		MongoOperations.replaceOne(Document.parse(dto.filter().encode()), convertToDocument(dto.value()), dto.id(), client, databaseName, dto.collectionName(), selectedBulkWriter);
	}

	@Override
	public void updateOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		MongoOperations.updateOne(Document.parse(dto.filter().encode()), Document.parse(dto.update().encode()), dto.id(), client, databaseName, dto.collectionName(), selectedBulkWriter);
	}

	@Override
	public void deleteOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		MongoOperations.deleteOne(Document.parse(dto.filter().encode()), dto.id(), client, databaseName, dto.collectionName(), selectedBulkWriter);
	}

	@Override
	public boolean handleMessage(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		boolean result = false;
		
		if (msg.tag()==FLUSH && bulkWrite) {
			selectedBulkWriter.flush();
			result = true;
		}
		
		return result;
	}

	@Override
	public void onSuccess(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		if (!bulkWrite && msg.tag()!=FIND_ONE && msg.tag()!=ActorWithCache.GET && msg.tag()!=HAS_ONE)
			dataAccess.tell(PersistentSuccessDTO.of(dto, msg.tag()), SUCCESS, msg.source(), msg.interaction());
	}

	@Override
	public void onFailure(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto, Throwable t) {
		// Inclusively invocation timeout regarding MongoClient, Retryable Reads/Writes
		// @See https://www.mongodb.com/docs/drivers/java/sync/v4.11/fundamentals/connection/mongoclientsettings/
		if (!bulkWrite)
			dataAccess.tell(PersistentFailureDTO.of(dto, msg.tag(), t), FAILURE, msg.source(), msg.interaction());
	}
}
