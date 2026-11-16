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
package io.actor4j.database.jpa;

import static io.actor4j.core.messages.ActorReservedTag.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.actor4j.core.utils.Pair;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

// not thread-safe, see ConcurrentJPABatchWriterImpl
// ordered: one transaction per batch (all or nothing), unordered: on failure each request in its own transaction
// each transaction uses its own EntityManager from the factory (no growing persistence context, no managed entities afterwards)
public class JPABatchWriterImpl<K, E> implements JPABatchWriter<K, E> {
	protected final EntityManagerFactory entityManagerFactory;
	protected final Class<E> entityType;

	protected List<Pair<UUID, JPAWriteModel>> requests;
	protected final boolean ordered;

	protected final int size;

	protected final Consumer<List<Pair<UUID, JPAWriteModel>>> onSuccess;
	protected final BiConsumer<List<Pair<UUID, JPAWriteModel>>, Throwable> onError;

	public JPABatchWriterImpl(EntityManagerFactory entityManagerFactory, Class<E> entityType, boolean ordered, int size,
			Consumer<List<Pair<UUID, JPAWriteModel>>> onSuccess, BiConsumer<List<Pair<UUID, JPAWriteModel>>, Throwable> onError) {
		super();
		if (size<1)
			throw new IllegalArgumentException("size must be at least 1: "+size);

		this.entityManagerFactory = entityManagerFactory;
		this.entityType = entityType;
		this.ordered = ordered;
		this.size = size;
		this.onSuccess = onSuccess;
		this.onError = onError;

		requests = new ArrayList<>();
	}

	@Override
	public void write(JPAWriteModel request, UUID id) {
		requests.add(Pair.of(id, request));
		if (requests.size()>=size)
			flush();
	}

	@Override
	public void flush() {
		List<Pair<UUID, JPAWriteModel>> batch = take();
		if (batch!=null)
			batchWrite(batch);
	}

	// the buffered requests (buffer is empty afterwards), null if there are none
	protected List<Pair<UUID, JPAWriteModel>> take() {
		if (requests.isEmpty())
			return null;

		List<Pair<UUID, JPAWriteModel>> result = requests;
		requests = new ArrayList<>();

		return result;
	}

	// exceptions of the callbacks are passed on, the buffer is already empty
	protected void batchWrite(List<Pair<UUID, JPAWriteModel>> batch) {
		try {
			transaction(batch);
		}
		catch(RuntimeException e) {
			if (ordered)
				error(batch, e);
			else
				oneByOne(batch);
			return;
		}

		if (onSuccess!=null)
			onSuccess.accept(batch);
	}

	protected void oneByOne(List<Pair<UUID, JPAWriteModel>> batch) {
		List<Pair<UUID, JPAWriteModel>> succeeded = new ArrayList<>();
		List<Pair<Pair<UUID, JPAWriteModel>, RuntimeException>> failed = new ArrayList<>();
		for (Pair<UUID, JPAWriteModel> request : batch)
			try {
				transaction(List.of(request));
				succeeded.add(request);
			}
			catch(RuntimeException e) {
				failed.add(Pair.of(request, e));
			}

		try {
			if (!succeeded.isEmpty() && onSuccess!=null)
				onSuccess.accept(succeeded);
		}
		finally {
			for (Pair<Pair<UUID, JPAWriteModel>, RuntimeException> pair : failed)
				error(List.of(pair.a()), pair.b());
		}
	}

	// commit before any callback, rollback on failure, a new EntityManager per transaction
	protected void transaction(List<Pair<UUID, JPAWriteModel>> batch) {
		try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
			EntityTransaction transaction = entityManager.getTransaction();
			transaction.begin();
			try {
				for (Pair<UUID, JPAWriteModel> request : batch)
					write(entityManager, request.b());
				transaction.commit();
			}
			catch(RuntimeException e) {
				if (transaction.isActive())
					transaction.rollback();
				throw e;
			}
		}
	}

	protected void write(EntityManager entityManager, JPAWriteModel request) {
		if (request.op()==RESERVED_DATA_ACCESS_INSERT_ONE)
			entityManager.persist(request.obj()/*value*/);
		else if (request.op()==RESERVED_DATA_ACCESS_REPLACE_ONE
			|| request.op()==RESERVED_DATA_ACCESS_UPDATE_ONE)
			entityManager.merge(request.obj()/*value*/);
		else if (request.op()==RESERVED_DATA_ACCESS_DELETE_ONE) {
			E reference = entityManager.getReference(entityType, request.obj()/*primaryKey*/);
			entityManager.remove(reference);
		}
		else
			throw new IllegalArgumentException("unknown operation: "+request.op());
	}

	protected void error(List<Pair<UUID, JPAWriteModel>> batch, Throwable t) {
		if (onError!=null)
			onError.accept(batch, t);
		else
			t.printStackTrace();
	}
}
