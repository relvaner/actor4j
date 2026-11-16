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
// replace, update and delete of a missing key change nothing and go to onNotFound (to onSuccess, if there is none)
public class JPABatchWriterImpl<K, E> implements JPABatchWriter<K, E> {
	protected final EntityManagerFactory entityManagerFactory;
	protected final Class<E> entityType;

	protected List<Pair<UUID, JPAWriteModel>> requests;
	protected final boolean ordered;

	protected final int size;

	protected final Consumer<List<Pair<UUID, JPAWriteModel>>> onSuccess;
	protected final BiConsumer<List<Pair<UUID, JPAWriteModel>>, Throwable> onError;
	protected final Consumer<List<Pair<UUID, JPAWriteModel>>> onNotFound;

	public JPABatchWriterImpl(EntityManagerFactory entityManagerFactory, Class<E> entityType, boolean ordered, int size,
			Consumer<List<Pair<UUID, JPAWriteModel>>> onSuccess, BiConsumer<List<Pair<UUID, JPAWriteModel>>, Throwable> onError) {
		this(entityManagerFactory, entityType, ordered, size, onSuccess, onError, null);
	}

	public JPABatchWriterImpl(EntityManagerFactory entityManagerFactory, Class<E> entityType, boolean ordered, int size,
			Consumer<List<Pair<UUID, JPAWriteModel>>> onSuccess, BiConsumer<List<Pair<UUID, JPAWriteModel>>, Throwable> onError,
			Consumer<List<Pair<UUID, JPAWriteModel>>> onNotFound) {
		super();
		if (size<1)
			throw new IllegalArgumentException("size must be at least 1: "+size);

		this.entityManagerFactory = entityManagerFactory;
		this.entityType = entityType;
		this.ordered = ordered;
		this.size = size;
		this.onSuccess = onSuccess;
		this.onError = onError;
		this.onNotFound = onNotFound;

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
		List<Pair<UUID, JPAWriteModel>> notFound;
		try {
			notFound = transaction(batch);
		}
		catch(RuntimeException e) {
			if (ordered)
				error(batch, e);
			else
				oneByOne(batch);
			return;
		}

		success(batch, notFound);
	}

	protected void oneByOne(List<Pair<UUID, JPAWriteModel>> batch) {
		List<Pair<UUID, JPAWriteModel>> written = new ArrayList<>();
		List<Pair<UUID, JPAWriteModel>> notFound = new ArrayList<>();
		List<Pair<Pair<UUID, JPAWriteModel>, RuntimeException>> failed = new ArrayList<>();
		for (Pair<UUID, JPAWriteModel> request : batch)
			try {
				notFound.addAll(transaction(List.of(request)));
				written.add(request);
			}
			catch(RuntimeException e) {
				failed.add(Pair.of(request, e));
			}

		try {
			success(written, notFound);
		}
		finally {
			for (Pair<Pair<UUID, JPAWriteModel>, RuntimeException> pair : failed)
				error(List.of(pair.a()), pair.b());
		}
	}

	// requests of the batch without the not found ones go to onSuccess, the not found ones to onNotFound
	protected void success(List<Pair<UUID, JPAWriteModel>> batch, List<Pair<UUID, JPAWriteModel>> notFound) {
		List<Pair<UUID, JPAWriteModel>> succeeded = batch;
		if (!notFound.isEmpty() && onNotFound!=null) {
			succeeded = new ArrayList<>(batch);
			succeeded.removeAll(notFound);
		}

		try {
			if (!succeeded.isEmpty() && onSuccess!=null)
				onSuccess.accept(succeeded);
		}
		finally {
			if (!notFound.isEmpty() && onNotFound!=null)
				onNotFound.accept(notFound);
		}
	}

	// commit before any callback, rollback on failure, a new EntityManager per transaction, returns the not found requests
	protected List<Pair<UUID, JPAWriteModel>> transaction(List<Pair<UUID, JPAWriteModel>> batch) {
		List<Pair<UUID, JPAWriteModel>> result = new ArrayList<>();

		try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
			EntityTransaction transaction = entityManager.getTransaction();
			transaction.begin();
			try {
				for (Pair<UUID, JPAWriteModel> request : batch)
					if (!write(entityManager, request.b()))
						result.add(request);
				transaction.commit();
			}
			catch(RuntimeException e) {
				if (transaction.isActive())
					transaction.rollback();
				throw e;
			}
		}

		return result;
	}

	// false if the key of a replace, update or delete does not exist (nothing is changed then)
	protected boolean write(EntityManager entityManager, JPAWriteModel request) {
		if (request.op()==RESERVED_DATA_ACCESS_INSERT_ONE)
			entityManager.persist(request.obj()/*value*/);
		else if (request.op()==RESERVED_CACHE_SET)
			entityManager.merge(request.obj()/*value*/);
		else if (request.op()==RESERVED_DATA_ACCESS_REPLACE_ONE
			|| request.op()==RESERVED_DATA_ACCESS_UPDATE_ONE) {
			if (!JPAOperations.exists(request.obj(), entityManager))
				return false;
			entityManager.merge(request.obj()/*value*/);
		}
		else if (request.op()==RESERVED_DATA_ACCESS_DELETE_ONE) {
			E entity = entityManager.find(entityType, request.obj()/*primaryKey*/);
			if (entity==null)
				return false;
			entityManager.remove(entity);
		}
		else
			throw new IllegalArgumentException("unknown operation: "+request.op());

		return true;
	}

	protected void error(List<Pair<UUID, JPAWriteModel>> batch, Throwable t) {
		if (onError!=null)
			onError.accept(batch, t);
		else
			t.printStackTrace();
	}
}
