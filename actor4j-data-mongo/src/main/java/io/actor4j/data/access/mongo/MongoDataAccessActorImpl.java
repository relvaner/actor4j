/*
 * Copyright (c) 2015-2026, David A. Bauer. All rights reserved.
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
package io.actor4j.data.access.mongo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.model.WriteModel;

import io.actor4j.core.actors.ActorRef;
import io.actor4j.core.actors.ActorWithCache;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.Pair;
import io.actor4j.data.access.BaseDataAccessActorImpl;
import io.actor4j.data.access.DocPersistentContext;
import io.actor4j.data.access.PersistentDataAccessDTO;
import io.actor4j.data.access.PersistentFailureDTO;
import io.actor4j.data.access.PersistentSuccessDTO;
import io.actor4j.database.mongo.MongoBufferedBulkWriter;
import io.actor4j.database.mongo.MongoOperations;

import static io.actor4j.data.access.DataAccessActor.*;
import static io.actor4j.database.mongo.MongoOperations.*;

// bulk mode: writes are answered by the callbacks of the bulk writer, everything else directly
public class MongoDataAccessActorImpl<K, E> extends BaseDataAccessActorImpl<K, E> {
	protected record BulkWriterRequest<K, E>(int tag, UUID interaction, ActorId source, PersistentDataAccessDTO<K, E> dto) {
	}

	protected final MongoClient client;
	protected final String databaseName;
	protected final boolean bulkWrite;
	protected final boolean bulkOrdered;
	protected final int bulkSize;
	protected final Class<E> entityType;
	protected final Map<String, MongoBufferedBulkWriter> bulkWriters;
	protected final Map<UUID, BulkWriterRequest<K, E>> bulkWriterRequests; // write id -> request

	protected MongoBufferedBulkWriter selectedBulkWriter;
	protected boolean notFound; // the current message was answered with FIND_NONE

	public MongoDataAccessActorImpl(ActorRef dataAccess, MongoClient client, String databaseName,
			boolean bulkWrite, boolean bulkOrdered, int bulkSize, Class<E> entityType, int maxFailures, long resetTimeout) {
		super(dataAccess, maxFailures, resetTimeout);

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
			BulkWriterRequest<K, E> originRequest = bulkWriterRequests.remove(pair.a()/*write id*/);
			if (originRequest!=null)
				dataAccess.tell(PersistentSuccessDTO.of(originRequest.dto(), originRequest.tag()), SUCCESS, originRequest.source(), originRequest.interaction());
		}
	}

	public void onBulkWriterError(List<Pair<UUID, WriteModel<Document>>> requests, Throwable t) {
		for (Pair<UUID, WriteModel<Document>> pair : requests) {
			BulkWriterRequest<K, E> originRequest = bulkWriterRequests.remove(pair.a()/*write id*/);
			if (originRequest!=null)
				dataAccess.tell(PersistentFailureDTO.of(originRequest.dto(), originRequest.tag(), t), FAILURE, originRequest.source(), originRequest.interaction());
		}
	}

	// writes the buffered requests (their senders get a reply), the MongoClient belongs to the caller
	public void close() {
		for (MongoBufferedBulkWriter bulkWriter : bulkWriters.values())
			try {
				bulkWriter.close();
			}
			catch(RuntimeException e) {
				e.printStackTrace();
			}
	}

	@Override
	public void onReceiveMessage(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		selectedBulkWriter = null;
		notFound = false;
		
		if (bulkWrite)
			if (dto.context() instanceof DocPersistentContext ctx) {
				MongoBufferedBulkWriter bulkWriter = bulkWriters.get(ctx.collectionName());
				if (bulkWriter==null) {
					bulkWriter = MongoBufferedBulkWriter.create(client, databaseName, ctx.collectionName(), bulkOrdered, bulkSize,
						this::onBulkWriterSuccess, this::onBulkWriterError);
					bulkWriters.put(ctx.collectionName(), bulkWriter);
				}

				selectedBulkWriter = bulkWriter;
			}
			else if (msg.tag()!=FLUSH) // FLUSH without context: all collections
				throw new IllegalArgumentException("Wrong context");
	}

	// a new id per write (copies of a DTO share its id), registered right before the write
	protected UUID register(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		if (selectedBulkWriter==null)
			return dto.id();

		UUID writeId = UUID.randomUUID();
		bulkWriterRequests.put(writeId, new BulkWriterRequest<>(msg.tag(), msg.interaction(), msg.source(), dto));

		return writeId;
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
		if (dto.context() instanceof DocPersistentContext ctx) {
			E entity = convertToEntity(MongoOperations.findOne(filterWithPrimary(dto.key(), ctx), client, databaseName, ctx.collectionName()), entityType);
			if (entity!=null)
				dataAccess.tell(dto.shallowCopy(entity), FIND_ONE, msg.source(), msg.interaction());
			else
				dataAccess.tell(dto, FIND_NONE, msg.source(), msg.interaction());
		}
		else
			throw new IllegalArgumentException("Wrong context");
	}

	@Override
	public void findAll(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		if (dto.context() instanceof DocPersistentContext ctx) {
			List<E> entities = convertToEntities(MongoOperations.findAll(ctx.filter()!=null ? Document.parse(ctx.filter().encode()) : null, client, databaseName, ctx.collectionName()), entityType);
			if (entities!=null)
				dataAccess.tell(dto.shallowCopyWithEntities(entities), FIND_ALL, msg.source(), msg.interaction());
			else
				dataAccess.tell(dto, FIND_NONE, msg.source(), msg.interaction());
		}
		else
			throw new IllegalArgumentException("Wrong context");
	}

	@Override
	public boolean hasOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		if (dto.context() instanceof DocPersistentContext ctx)
			return MongoOperations.hasOne(filterWithPrimary(dto.key(), ctx), client, databaseName, ctx.collectionName());
		else
			throw new IllegalArgumentException("Wrong context");
	}

	// SET is written as upsert: idempotent, no duplicates if the insert is still in the bulk buffer
	@Override
	public void insertOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		if (msg.tag()==ActorWithCache.SET)
			upsertOne(msg, dto);
		else if (dto.context() instanceof DocPersistentContext ctx) {
			Document document = convertToDocument(dto.value());
			MongoOperations.insertOne(document, register(msg, dto), client, databaseName, ctx.collectionName(), selectedBulkWriter);
		}
		else
			throw new IllegalArgumentException("Wrong context");
	}

	@Override
	public void replaceOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		if (msg.tag()==ActorWithCache.SET)
			upsertOne(msg, dto);
		else if (dto.context() instanceof DocPersistentContext ctx) {
			Document document = convertToDocument(dto.value());
			found(MongoOperations.replaceOne(filterWithPrimary(dto.key(), ctx), document, register(msg, dto), client, databaseName, ctx.collectionName(), selectedBulkWriter), msg, dto);
		}
		else
			throw new IllegalArgumentException("Wrong context");
	}

	protected void upsertOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		if (dto.context() instanceof DocPersistentContext ctx) {
			Document document = convertToDocument(dto.value());
			MongoOperations.upsertOne(filterWithPrimary(dto.key(), ctx), document, register(msg, dto), client, databaseName, ctx.collectionName(), selectedBulkWriter);
		}
		else
			throw new IllegalArgumentException("Wrong context");
	}

	@Override
	public void updateOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		if (dto.context() instanceof DocPersistentContext ctx) {
			Document update = Document.parse(ctx.update().encode());
			found(MongoOperations.updateOne(filterWithPrimary(dto.key(), ctx), update, register(msg, dto), client, databaseName, ctx.collectionName(), selectedBulkWriter), msg, dto);
		}
		else
			throw new IllegalArgumentException("Wrong context");
	}

	@Override
	public void deleteOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		if (dto.context() instanceof DocPersistentContext ctx)
			found(MongoOperations.deleteOne(filterWithPrimary(dto.key(), ctx), register(msg, dto), client, databaseName, ctx.collectionName(), selectedBulkWriter), msg, dto);
		else
			throw new IllegalArgumentException("Wrong context");
	}

	// replace, update or delete of a missing key: nothing changed, answered with FIND_NONE instead of SUCCESS
	// (in bulk mode always found, the bulk result has only totals)
	protected void found(boolean found, ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		if (!found) {
			dataAccess.tell(dto, FIND_NONE, msg.source(), msg.interaction());
			notFound = true;
		}
	}

	// FLUSH with a context writes this collection, without a context all collections
	@Override
	public boolean handleMessage(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		boolean result = false;

		if (msg.tag()==FLUSH && bulkWrite) {
			if (selectedBulkWriter!=null)
				selectedBulkWriter.flush();
			else
				bulkWriters.values().forEach(MongoBufferedBulkWriter::flush);
			result = true;
		}

		return result;
	}

	// reads answer with their result, in bulk mode writes are answered by the bulk writer
	@Override
	public void onSuccess(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		if (!bulkWrite && !isRead(msg.tag()) && !notFound)
			dataAccess.tell(PersistentSuccessDTO.of(dto, msg.tag()), SUCCESS, msg.source(), msg.interaction());
	}

	// every failure is answered (failures of buffered writes are answered by the bulk writer)
	@Override
	public void onFailure(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto, Throwable t) {
		// Inclusively invocation timeout regarding MongoClient, Retryable Reads/Writes
		// @See https://www.mongodb.com/docs/drivers/java/sync/v4.11/fundamentals/connection/mongoclientsettings/
		dataAccess.tell(PersistentFailureDTO.of(dto, msg.tag(), t), FAILURE, msg.source(), msg.interaction());
	}

	protected static boolean isRead(int tag) {
		return tag==FIND_ONE || tag==ActorWithCache.GET || tag==HAS_ONE || tag==FIND_ALL || tag==ActorWithCache.GET_ALL || tag==QUERY_ONE || tag==QUERY_ALL;
	}

	// a copy of the filter of the context (the context is shared), plus the primary key
	protected Document filterWithPrimary(K key, DocPersistentContext ctx) {
		Document result = ctx.filter()!=null ? Document.parse(ctx.filter().encode()) : new Document();
		if (ctx.keyName()!=null)
			result.put(ctx.keyName(), key);

		return result;
	}
}
