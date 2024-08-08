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

import static io.actor4j.core.data.access.DataAccessActor.FAILURE;
import static io.actor4j.core.data.access.DataAccessActor.SUCCESS;

import io.actor4j.core.actors.ActorRef;
import io.actor4j.core.data.access.BaseDataAccessActorImpl;
import io.actor4j.core.data.access.PersistentDataAccessDTO;
import io.actor4j.core.data.access.PersistentFailureDTO;
import io.actor4j.core.data.access.PersistentSuccessDTO;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.database.jpa.JPAOperations;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class JPADataAccessActorImpl<K, E> extends BaseDataAccessActorImpl<K, E> {
	protected final String persistenceUnitName;
	protected final Class<E> entityType;
	
	protected EntityManagerFactory entityManagerFactory;
	protected EntityManager entityManager;
	
	public JPADataAccessActorImpl(ActorRef dataAccess, String persistenceUnitName, Class<E> entityType, int maxFailures, long resetTimeout) {
		super(dataAccess, maxFailures, resetTimeout);
		
		this.persistenceUnitName = persistenceUnitName;
		this.entityType = entityType;
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

	@Override
	public void onReceiveMessage(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
	}
	
	public void queryOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		JPAOperations.queryOne(dto.query(), entityType, entityManager);
	}

	@Override
	public void onFindOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		JPAOperations.findOne(dto.key(), entityType, entityManager);
	}

	@Override
	public boolean hasOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		return JPAOperations.hasOne(dto.key(), entityType, entityManager);
	}

	@Override
	public void insertOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		JPAOperations.insertOne(dto.value(), dto.id(), entityManager);
	}

	@Override
	public void replaceOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		JPAOperations.replaceOne(dto.value(), dto.id(), entityManager);
	}

	@Override
	public void updateOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		JPAOperations.updateOne(dto.value(), dto.id(), entityManager);
	}

	@Override
	public void deleteOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		JPAOperations.deleteOne(dto.key(), entityType, dto.id(), entityManager);
	}

	@Override
	public boolean handleMessage(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		return false;
	}

	@Override
	public void onSuccess(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto) {
		dataAccess.tell(PersistentSuccessDTO.of(dto, msg.tag()), SUCCESS, msg.source(), msg.interaction());
	}

	@Override
	public void onFailure(ActorMessage<?> msg, PersistentDataAccessDTO<K, E> dto, Throwable t) {
		dataAccess.tell(PersistentFailureDTO.of(dto, msg.tag(), t), FAILURE, msg.source(), msg.interaction());
	}
}
