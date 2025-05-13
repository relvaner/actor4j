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
package io.actor4j.core.data.access.jpa;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import io.actor4j.core.actors.ActorRef;
import io.actor4j.core.actors.ActorWithCache;
import io.actor4j.core.data.access.BaseDataAccessActorImpl;
import io.actor4j.core.data.access.PersistentContext;
import io.actor4j.core.data.access.PersistentDataAccessDTO;
import io.actor4j.core.data.access.PersistentFailureDTO;
import io.actor4j.core.data.access.PersistentSuccessDTO;
import io.actor4j.core.data.access.SqlPersistentContext;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.Pair;
import io.actor4j.database.jpa.JPABatchWriter;
import io.actor4j.database.jpa.JPAOperations;
import io.actor4j.database.jpa.JPAWriteModel;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import static io.actor4j.core.data.access.DataAccessActor.*;

public class JPADataAccessActorImpl<K, E> extends BaseDataAccessActorImpl<K, E> {
	protected record BatchWriterRequest<K, E>(int tag, UUID interaction, ActorId source, PersistentDataAccessDTO<K, E> dto) {
	}
	
	protected EntityManagerFactory entityManagerFactory;
	protected EntityManager entityManager;
	
	protected final String persistenceUnitName;
	protected final boolean batchWrite;
	protected final boolean batchOrdered;
	protected final int batchSize;
	protected final Map<String, Class<E>> entityTypes;
	protected final Map<String, JPABatchWriter<K, E>> batchWriters;
	protected final Map<UUID, BatchWriterRequest<K, E>> batchWriterRequests; // id -> request
	
	protected JPABatchWriter<K, E> selectedBatchWriter;
	
	public JPADataAccessActorImpl(ActorRef dataAccess, String persistenceUnitName, boolean batchWrite, 
			boolean batchOrdered, int batchSize, Class<E> entityType, int maxFailures, long resetTimeout) {
		this(dataAccess, persistenceUnitName, batchWrite, batchOrdered, batchSize, Map.of(entityType.getClass().getSimpleName(), entityType), maxFailures, resetTimeout);
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
	
	protected Pair<String, Class<E>> getEntityTypeAsPair(PersistentContext context) {
		if (context==null && entityTypes.size()==1) {
			Entry<String, Class<E>> entry = entityTypes.entrySet().iterator().next();
			return Pair.of(entry.getKey(), entry.getValue());
		}
		else if (context instanceof SqlPersistentContext ctx)
			return Pair.of(ctx.entityName(), entityTypes.get(ctx.entityName()));
		else
			throw new IllegalArgumentException("Wrong context");
	}
	
	protected Class<E> getEntityType(PersistentContext context) {
		if (context==null && entityTypes.size()==1)
			return entityTypes.entrySet().iterator().next().getValue();
		else if (context instanceof SqlPersistentContext ctx)
			return entityTypes.get(ctx.entityName());
		else
			throw new IllegalArgumentException("Wrong context");
	}
	
	public void preStart() {
		EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory(persistenceUnitName);
		entityManager = entityManagerFactory.createEntityManager();
	}
	
	public void close() {
		if (entityManager!=null)
			entityManager.close();
		if (entityManagerFactory!=null)
			entityManagerFactory.close();
	}
	
	public void onBatchWriterSuccess(List<Pair<UUID, JPAWriteModel>> requests) {
		for (Pair<UUID, JPAWriteModel> pair : requests) {
			BatchWriterRequest<K, E> originRequest = batchWriterRequests.get(pair.a()/*id*/);
			dataAccess.tell(PersistentSuccessDTO.of(originRequest.dto(), originRequest.tag()), SUCCESS, originRequest.source(), originRequest.interaction());
			
			batchWriterRequests.remove(pair.a());
		}
	}
	
	public void onBatchWriterError(List<Pair<UUID, JPAWriteModel>> requests, Throwable t) {
		for (Pair<UUID, JPAWriteModel> pair : requests) {
			BatchWriterRequest<K, E> originRequest = batchWriterRequests.get(pair.a()/*id*/);
			dataAccess.tell(PersistentFailureDTO.of(originRequest.dto(), originRequest.tag(), t), FAILURE, originRequest.source(), originRequest.interaction());
			
			batchWriterRequests.remove(pair.a());
		}
	}

	@Override
	public void onReceiveMessage(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		if (batchWrite) {
			Pair<String, Class<E>> pair = getEntityTypeAsPair(dto.context());
			
			JPABatchWriter<K, E> batchWriter = batchWriters.get(pair.key());
			if (batchWriter==null) {
				batchWriter = JPABatchWriter.create(entityManager, pair.value(), batchOrdered, batchSize, 
					this::onBatchWriterSuccess, this::onBatchWriterError);
				batchWriters.put(pair.key(), batchWriter);
			}
			
			selectedBatchWriter = batchWriter;
			
			if (msg.tag()!=FIND_ONE && msg.tag()!=ActorWithCache.GET && msg.tag()!=HAS_ONE)
				batchWriterRequests.put(dto.id(), new BatchWriterRequest<>(msg.tag(), msg.interaction(), msg.source(), dto));
		}	
	}
	
	@Override
	public void queryOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		if (dto.context() instanceof SqlPersistentContext ctx) {
			E entity = JPAOperations.queryOne(ctx.query(), getEntityType(ctx), entityManager);
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
			List<E> entities = JPAOperations.queryAll(ctx.query(), getEntityType(ctx), entityManager);
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
		E entity = JPAOperations.findOne(dto.key(), getEntityType(dto.context()), entityManager);
		if (entity!=null)
			dataAccess.tell(dto.shallowCopy(entity), FIND_ONE, msg.source(), msg.interaction());
		else
			dataAccess.tell(dto, FIND_NONE, msg.source(), msg.interaction());
	}

	@Override
	public void findAll(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		List<E> entities = JPAOperations.findAll(getEntityType(dto.context()), entityManager);
		if (entities!=null)
			dataAccess.tell(dto.shallowCopyWithEntities(entities), FIND_ALL, msg.source(), msg.interaction());
		else
			dataAccess.tell(dto, FIND_NONE, msg.source(), msg.interaction());
	}
	
	@Override
	public boolean hasOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		return JPAOperations.hasOne(dto.key(), getEntityType(dto.context()), entityManager);
	}

	@Override
	public void insertOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		JPAOperations.insertOne(dto.entity(), dto.id(), entityManager, selectedBatchWriter);
	}

	@Override
	public void replaceOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		JPAOperations.replaceOne(dto.entity(), dto.id(), entityManager, selectedBatchWriter);
	}

	@Override
	public void updateOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		JPAOperations.updateOne(dto.entity(), dto.id(), entityManager, selectedBatchWriter);
	}

	@Override
	public void deleteOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		JPAOperations.deleteOne(dto.key(), getEntityType(dto.context()), dto.id(), entityManager, selectedBatchWriter);
	}

	@Override
	public boolean handleMessage(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		boolean result = false;
		
		if (msg.tag()==FLUSH && batchWrite) {
			selectedBatchWriter.flush();
			result = true;
		}
		
		return result;	
	}

	@Override
	public void onSuccess(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		if (!batchWrite && msg.tag()!=FIND_ONE && msg.tag()!=ActorWithCache.GET && msg.tag()!=HAS_ONE)
			dataAccess.tell(PersistentSuccessDTO.of(dto, msg.tag()), SUCCESS, msg.source(), msg.interaction());
	}

	@Override
	public void onFailure(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto, Throwable t) {
		if (!batchWrite)
			dataAccess.tell(PersistentFailureDTO.of(dto, msg.tag(), t), FAILURE, msg.source(), msg.interaction());
	}
}
