/*
 * Copyright (c) 2015-2026,David A. Bauer. All rights reserved.
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
package io.actor4j.data.access.jpa;

import static io.actor4j.data.access.DataAccessActor.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.function.Function;

import io.actor4j.core.actors.ActorRef;
import io.actor4j.core.actors.ActorWithCache;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.Pair;
import io.actor4j.data.access.BaseDataAccessActorImpl;
import io.actor4j.data.access.PersistentContext;
import io.actor4j.data.access.PersistentDataAccessDTO;
import io.actor4j.data.access.PersistentFailureDTO;
import io.actor4j.data.access.PersistentSuccessDTO;
import io.actor4j.data.access.SqlPersistentContext;
import io.actor4j.database.jpa.JPABatchWriter;
import io.actor4j.database.jpa.JPAOperations;
import io.actor4j.database.jpa.JPAWriteModel;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

// batch mode: writes are answered by the callbacks of the batch writer, everything else directly
// a new EntityManager per operation: no growing persistence context, no stale reads, returned entities are detached
public class JPADataAccessActorImpl<K, E> extends BaseDataAccessActorImpl<K, E> {
	protected record BatchWriterRequest<K, E>(int tag, UUID interaction, ActorId source, PersistentDataAccessDTO<K, E> dto) {
	}
	
	protected EntityManagerFactory entityManagerFactory;

	protected final String persistenceUnitName;
	protected final boolean batchWrite;
	protected final boolean batchOrdered;
	protected final int batchSize;
	protected final Map<String, Class<E>> entityTypes;
	protected final Map<String, JPABatchWriter<K, E>> batchWriters;
	protected final Map<UUID, BatchWriterRequest<K, E>> batchWriterRequests; // write id -> request
	
	protected JPABatchWriter<K, E> selectedBatchWriter;
	protected boolean notFound; // the current message was answered with FIND_NONE

	public JPADataAccessActorImpl(ActorRef dataAccess, String persistenceUnitName, boolean batchWrite, 
			boolean batchOrdered, int batchSize, Class<E> entityType, int maxFailures, long resetTimeout) {
		this(dataAccess, persistenceUnitName, batchWrite, batchOrdered, batchSize, Map.of(entityName(entityType), entityType), maxFailures, resetTimeout);
	}
	
	public JPADataAccessActorImpl(ActorRef dataAccess, String persistenceUnitName, boolean batchWrite, 
			boolean batchOrdered, int batchSize, Map<String, Class<E>> entityTypes, int maxFailures, long resetTimeout) {
		super(dataAccess, maxFailures, resetTimeout);
		
		this.persistenceUnitName = persistenceUnitName;
		this.batchWrite = batchWrite;
		this.batchOrdered = batchOrdered;
		this.batchSize = batchSize;
		this.entityTypes = entityTypes;
		
		batchWriters = new HashMap<>();
		batchWriterRequests = new HashMap<>();
	}
	
	// the entity name as used in JPQL: @Entity(name=...) or the simple class name
	public static String entityName(Class<?> entityType) {
		Entity entity = entityType.getAnnotation(Entity.class);

		return entity!=null && !entity.name().isEmpty() ? entity.name() : entityType.getSimpleName();
	}

	protected Pair<String, Class<E>> getEntityTypeAsPair(PersistentContext context) {
		if (context==null && entityTypes.size()==1) {
			Entry<String, Class<E>> entry = entityTypes.entrySet().iterator().next();
			return Pair.of(entry.getKey(), entry.getValue());
		}
		else if (context instanceof SqlPersistentContext ctx) {
			Class<E> entityType = entityTypes.get(ctx.entityName());
			if (entityType==null)
				throw new IllegalArgumentException("Unknown entity: "+ctx.entityName());
			return Pair.of(ctx.entityName(), entityType);
		}
		else
			throw new IllegalArgumentException("Wrong context");
	}

	protected Class<E> getEntityType(PersistentContext context) {
		return getEntityTypeAsPair(context).value();
	}

	public void preStart() {
		entityManagerFactory = Persistence.createEntityManagerFactory(persistenceUnitName);
	}

	protected <T> T withEntityManager(Function<EntityManager, T> operation) {
		try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
			return operation.apply(entityManager);
		}
	}

	// in batch mode the batch writer opens its own EntityManager per transaction
	protected <T> T write(Function<EntityManager, T> operation) {
		if (selectedBatchWriter!=null)
			return operation.apply(null);
		else
			return withEntityManager(operation);
	}

	// replace, update or delete of a missing key: nothing changed, answered with FIND_NONE instead of SUCCESS
	// (in batch mode reported by the batch writer)
	protected void found(boolean found, ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		if (!found) {
			dataAccess.tell(dto, FIND_NONE, msg.source(), msg.interaction());
			notFound = true;
		}
	}

	// writes the buffered requests first (their senders get a reply), then closes the factory
	public void close() {
		try {
			for (JPABatchWriter<K, E> batchWriter : batchWriters.values())
				try {
					batchWriter.close();
				}
				catch(RuntimeException e) {
					e.printStackTrace();
				}
		}
		finally {
			if (entityManagerFactory!=null)
				entityManagerFactory.close();
		}
	}
	
	public void onBatchWriterSuccess(List<Pair<UUID, JPAWriteModel>> requests) {
		for (Pair<UUID, JPAWriteModel> pair : requests) {
			BatchWriterRequest<K, E> originRequest = batchWriterRequests.remove(pair.a()/*write id*/);
			if (originRequest!=null)
				dataAccess.tell(PersistentSuccessDTO.of(originRequest.dto(), originRequest.tag()), SUCCESS, originRequest.source(), originRequest.interaction());
		}
	}

	public void onBatchWriterNotFound(List<Pair<UUID, JPAWriteModel>> requests) {
		for (Pair<UUID, JPAWriteModel> pair : requests) {
			BatchWriterRequest<K, E> originRequest = batchWriterRequests.remove(pair.a()/*write id*/);
			if (originRequest!=null)
				dataAccess.tell(originRequest.dto(), FIND_NONE, originRequest.source(), originRequest.interaction());
		}
	}

	public void onBatchWriterError(List<Pair<UUID, JPAWriteModel>> requests, Throwable t) {
		for (Pair<UUID, JPAWriteModel> pair : requests) {
			BatchWriterRequest<K, E> originRequest = batchWriterRequests.remove(pair.a()/*write id*/);
			if (originRequest!=null)
				dataAccess.tell(PersistentFailureDTO.of(originRequest.dto(), originRequest.tag(), t), FAILURE, originRequest.source(), originRequest.interaction());
		}
	}

	@Override
	public void onReceiveMessage(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		selectedBatchWriter = null;
		notFound = false;

		if (batchWrite && !(msg.tag()==FLUSH && dto.context()==null)) { // FLUSH without context: all entity types
			Pair<String, Class<E>> pair = getEntityTypeAsPair(dto.context());

			JPABatchWriter<K, E> batchWriter = batchWriters.get(pair.key());
			if (batchWriter==null) {
				batchWriter = JPABatchWriter.create(entityManagerFactory, pair.value(),batchOrdered, batchSize, 
					this::onBatchWriterSuccess, this::onBatchWriterError, this::onBatchWriterNotFound);
				batchWriters.put(pair.key(), batchWriter);
			}
			
			selectedBatchWriter = batchWriter;
		}
	}

	// a new id per write (copies of a DTO share its id), registered right before the write
	protected UUID register(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		if (selectedBatchWriter==null)
			return dto.id();

		UUID writeId = UUID.randomUUID();
		batchWriterRequests.put(writeId, new BatchWriterRequest<>(msg.tag(), msg.interaction(), msg.source(), dto));

		return writeId;
	}

	@Override
	public void queryOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		if (dto.context() instanceof SqlPersistentContext ctx) {
			E entity = withEntityManager(entityManager -> JPAOperations.queryOne(ctx.query(), getEntityType(ctx), entityManager));
			if (entity!=null)
				dataAccess.tell(dto.shallowCopy(entity), FIND_ONE, msg.source(), msg.interaction());
			else
				dataAccess.tell(dto, FIND_NONE, msg.source(), msg.interaction());
		}
		else
			throw new IllegalArgumentException("Wrong context");
	}
	
	@Override
	public void queryAll(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		if (dto.context() instanceof SqlPersistentContext ctx) {
			List<E> entities = withEntityManager(entityManager -> JPAOperations.queryAll(ctx.query(), getEntityType(ctx), entityManager));
			if (entities!=null)
				dataAccess.tell(dto.shallowCopyWithEntities(entities), FIND_ALL, msg.source(), msg.interaction());
			else
				dataAccess.tell(dto, FIND_NONE, msg.source(), msg.interaction());
		}
		else
			throw new IllegalArgumentException("Wrong context");
	}

	@Override
	public void findOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		E entity = withEntityManager(entityManager -> JPAOperations.findOne(dto.key(), getEntityType(dto.context()), entityManager));
		if (entity!=null)
			dataAccess.tell(dto.shallowCopy(entity), FIND_ONE, msg.source(), msg.interaction());
		else
			dataAccess.tell(dto, FIND_NONE, msg.source(), msg.interaction());
	}

	@Override
	public void findAll(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		List<E> entities = withEntityManager(entityManager -> JPAOperations.findAll(getEntityType(dto.context()), entityManager));
		if (entities!=null)
			dataAccess.tell(dto.shallowCopyWithEntities(entities), FIND_ALL, msg.source(), msg.interaction());
		else
			dataAccess.tell(dto, FIND_NONE, msg.source(), msg.interaction());
	}
	
	@Override
	public boolean hasOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		return withEntityManager(entityManager -> JPAOperations.hasOne(dto.key(), getEntityType(dto.context()), entityManager));
	}

	@Override
	// SET is written with merge (insert or update): idempotent, no failing batch if the insert is still in the buffer
	public void insertOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		UUID id = register(msg, dto);
		if (msg.tag()==ActorWithCache.SET)
			write(entityManager -> {
				JPAOperations.upsertOne(dto.entity(), id, entityManager, selectedBatchWriter);
				return null;
			});
		else
			write(entityManager -> {
				JPAOperations.insertOne(dto.entity(), id, entityManager, selectedBatchWriter);
				return null;
			});
	}

	@Override
	public void replaceOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		UUID id = register(msg, dto);
		found(write(entityManager -> JPAOperations.replaceOne(dto.entity(), id, entityManager, selectedBatchWriter)), msg, dto);
	}

	@Override
	public void updateOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		UUID id = register(msg, dto);
		found(write(entityManager -> JPAOperations.updateOne(dto.entity(), id, entityManager, selectedBatchWriter)), msg, dto);
	}

	@Override
	public void deleteOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		Class<E> entityType = getEntityType(dto.context());
		UUID id = register(msg, dto);
		found(write(entityManager -> JPAOperations.deleteOne(dto.key(), entityType, id, entityManager, selectedBatchWriter)), msg, dto);
	}

	@Override
	// FLUSH with a context writes this entity type, without a context all entity types
	public boolean handleMessage(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		boolean result = false;

		if (msg.tag()==FLUSH && batchWrite) {
			if (selectedBatchWriter!=null)
				selectedBatchWriter.flush();
			else
				batchWriters.values().forEach(JPABatchWriter::flush);
			result = true;
		}

		return result;	
	}

	// reads answer with their result, in batch mode writes are answered by the batch writer
	@Override
	public void onSuccess(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		if (!batchWrite && !isRead(msg.tag()) && !notFound)
			dataAccess.tell(PersistentSuccessDTO.of(dto, msg.tag()), SUCCESS, msg.source(), msg.interaction());
	}

	// every failure is answered (failures of buffered writes are answered by the batch writer)
	@Override
	public void onFailure(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto, Throwable t) {
		dataAccess.tell(PersistentFailureDTO.of(dto, msg.tag(), t), FAILURE, msg.source(), msg.interaction());
	}

	protected static boolean isRead(int tag) {
		return tag==FIND_ONE || tag==ActorWithCache.GET || tag==HAS_ONE || tag==FIND_ALL || tag==ActorWithCache.GET_ALL || tag==QUERY_ONE || tag==QUERY_ALL;
	}
}
