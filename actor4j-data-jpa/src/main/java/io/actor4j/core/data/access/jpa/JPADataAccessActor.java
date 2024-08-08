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

import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.data.access.DataAccessActor;

public class JPADataAccessActor<K, E> extends DataAccessActor<K, E> {
	protected JPADataAccessActorImpl<K, E> impl;
	
	public JPADataAccessActor(String name, String persistenceUnitName, boolean batchWrite, 
			boolean batchOrdered, int batchSize, Class<E> entityType, int maxFailures, long resetTimeout) {
		super(name, true); // @Stateful
		
		impl = new JPADataAccessActorImpl<K, E>(this, persistenceUnitName, batchWrite, batchOrdered, batchSize, entityType, maxFailures, resetTimeout);
	}

	public JPADataAccessActor(String persistenceUnitName, boolean batchWrite, 
			boolean batchOrdered, int batchSize, Class<E> entityType, int maxFailures, long resetTimeout) {
		this(null, persistenceUnitName, batchWrite, batchOrdered, batchSize, entityType, maxFailures, resetTimeout);
	}
	
	public JPADataAccessActor(String name, String persistenceUnitName, boolean batchWrite, 
			boolean batchOrdered, int batchSize, Class<E> entityType) {
		this(name, persistenceUnitName, batchWrite, batchOrdered, batchSize, entityType, DEFAULT_MAX_FAILURES, DEFAULT_RESET_TIMEOUT);
	}
	
	public JPADataAccessActor(String persistenceUnitName, boolean batchWrite, 
			boolean batchOrdered, int batchSize, Class<E> entityType) {
		this(null, persistenceUnitName, batchWrite, batchOrdered, batchSize, entityType, DEFAULT_MAX_FAILURES, DEFAULT_RESET_TIMEOUT);
	}
	
	public JPADataAccessActor(String name, String persistenceUnitName, Class<E> entityType, int maxFailures, long resetTimeout) {
		this(name, persistenceUnitName, false, true, 0, entityType, maxFailures, resetTimeout);
	}
	
	public JPADataAccessActor(String name, String persistenceUnitName, Class<E> entityType) {
		this(name, persistenceUnitName, entityType, DEFAULT_MAX_FAILURES, DEFAULT_RESET_TIMEOUT);
	}
	
	public JPADataAccessActor(String persistenceUnitName, Class<E> entityType) {
		this(null, persistenceUnitName, entityType, DEFAULT_MAX_FAILURES, DEFAULT_RESET_TIMEOUT);
	}
	
	@Override
	public void preStart() {
		impl.preStart();
	}

	@Override
	public void receive(ActorMessage<?> message) {
		impl.receive(message);
	}
	
	@Override
	public void postStop() {
		impl.close();
	}
}
