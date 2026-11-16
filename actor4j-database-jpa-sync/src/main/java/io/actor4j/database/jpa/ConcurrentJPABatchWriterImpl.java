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

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.actor4j.core.utils.Pair;
import jakarta.persistence.EntityManager;

// thread-safe: the buffer lock is held only briefly, batches are written one after another in their order
// the EntityManager is only used under writeLock, nobody else may use it at the same time
public class ConcurrentJPABatchWriterImpl<K, E> extends JPABatchWriterImpl<K, E> implements ConcurrentJPABatchWriter<K, E> {
	protected final Lock lock;
	protected final Lock writeLock;
	protected final Queue<List<Pair<UUID, JPAWriteModel>>> batches;

	public ConcurrentJPABatchWriterImpl(EntityManager entityManager, Class<E> entityType,
			boolean ordered, int size, Consumer<List<Pair<UUID, JPAWriteModel>>> onSuccess,
			BiConsumer<List<Pair<UUID, JPAWriteModel>>, Throwable> onError) {
		super(entityManager, entityType, ordered, size, onSuccess, onError);

		lock = new ReentrantLock();
		writeLock = new ReentrantLock();
		batches = new ArrayDeque<>();
	}

	@Override
	public void write(JPAWriteModel request, UUID id) {
		boolean full;
		lock.lock();
		try {
			requests.add(Pair.of(id, request));
			full = requests.size()>=size;
			if (full)
				batches.add(take());
		}
		finally {
			lock.unlock();
		}

		if (full)
			drain();
	}

	@Override
	public void flush() {
		lock.lock();
		try {
			List<Pair<UUID, JPAWriteModel>> batch = take();
			if (batch!=null)
				batches.add(batch);
		}
		finally {
			lock.unlock();
		}

		drain();
	}

	// writes all pending batches in FIFO order, without holding the buffer lock
	protected void drain() {
		writeLock.lock();
		try {
			List<Pair<UUID, JPAWriteModel>> batch;
			while ((batch = poll())!=null)
				batchWrite(batch);
		}
		finally {
			writeLock.unlock();
		}
	}

	protected List<Pair<UUID, JPAWriteModel>> poll() {
		lock.lock();
		try {
			return batches.poll();
		}
		finally {
			lock.unlock();
		}
	}
}
