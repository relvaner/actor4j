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
package io.actor4j.database.jpa;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import io.actor4j.core.utils.Pair;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import static io.actor4j.core.messages.ActorReservedTag.*;

public class JPABatchWriterImpl<K, E> implements JPABatchWriter<K, E> {
	protected final EntityManager entityManager;
	protected final Function<E, K> getKey;
	protected final Class<E> entityType;
	
	protected final Queue<Pair<UUID, Pair<Integer, E>>> requestsQueue;
	protected final boolean ordered;
	
	protected final int size;
	protected int count;
	
	protected final Consumer<List<Pair<UUID, Pair<Integer, E>>>> onSuccess;
	protected final BiConsumer<List<Pair<UUID, Pair<Integer, E>>>, Throwable> onError;

	public JPABatchWriterImpl(EntityManager entityManager, Function<E, K> getKey, Class<E> entityType, boolean ordered, int size,
			Consumer<List<Pair<UUID, Pair<Integer, E>>>> onSuccess, BiConsumer<List<Pair<UUID, Pair<Integer, E>>>, Throwable> onError) {
		super();
		this.entityManager = entityManager;
		this.getKey = getKey;
		this.entityType = entityType;
		this.ordered = ordered;
		this.size = size;
		this.onSuccess = onSuccess;
		this.onError = onError;
		
		count = 0;
		requestsQueue = new LinkedList<>();
	}
	
	@Override
	public void write(Pair<Integer, E> request, UUID id) {
		requestsQueue.offer(Pair.of(id, request));
		if (++count==size)
			flush();
	}
	
	protected void write(Pair<Integer, E> request) {
		if (request.left()==RESERVED_DATA_ACCESS_INSERT_ONE 
			|| request.left()==RESERVED_CACHE_SET)
			entityManager.persist(request.right());
		else if (request.left()==RESERVED_DATA_ACCESS_REPLACE_ONE 
			|| request.left()==RESERVED_DATA_ACCESS_UPDATE_ONE
			|| request.left()==RESERVED_CACHE_UPDATE)
			entityManager.merge(request.right());
		else if (request.left()==RESERVED_DATA_ACCESS_DELETE_ONE) {
			E reference = entityManager.getReference(entityType, getKey.apply(request.right()));
			entityManager.remove(reference);
		}
	}
	
	@Override
	public void flush() {
		List<Pair<UUID, Pair<Integer, E>>> writeRequests = new LinkedList<>(requestsQueue);
		
		EntityTransaction transaction = null;
		try {
			transaction = entityManager.getTransaction();
			transaction.begin();
			writeRequests.forEach((r) -> write(r.right()));
			try {
				entityManager.flush();
				
				if (onSuccess!=null)
					onSuccess.accept(writeRequests);
			}
			catch(Exception e) {
				e.printStackTrace();
				
				transaction.rollback();
				if (!ordered) {
					// Transactions Manually
					writeRequests.forEach((r) -> {
						EntityTransaction newTransaction = entityManager.getTransaction();
						try {
							newTransaction.begin();
							write(r.right());
							entityManager.flush();
						}
						catch (Exception ex) {
							e.printStackTrace();
							newTransaction.rollback();
						}
						finally {
							if (newTransaction!=null && newTransaction.isActive()) {
						        if (!newTransaction.getRollbackOnly())
						            newTransaction.commit();
						    }
							
							if (onError!=null)
								onError.accept(List.of(r), e);
						}
					});
				}
				else {
					if (onError!=null)
						onError.accept(writeRequests, e);
				}
			}
		}
		catch(Exception e) {
			e.printStackTrace();

			if (onError!=null)
				onError.accept(writeRequests, e);
		}
		finally {
			if (transaction!=null && transaction.isActive()) {
		        if (!transaction.getRollbackOnly())
		        	transaction.commit();
		    }
		}
		
		requestsQueue.clear();
		count = 0;
	}
}
