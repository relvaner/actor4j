/**
 *  Copyright 2011-2013 Terracotta, Inc.
 *  Copyright 2011-2013 Oracle America Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.actor4j.cache.runtime;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A factory for {@link ReentrantLock}s.
 * 
 * Provides a pool of ReentrantLocks to minimize object creation.
 */
// Changed implementation
public final class LockFactory {
	private final int capacity;
	private final ReentrantLock lockFactoryLock;
	private final Queue<ReentrantLock> lockPool;
	
	public LockFactory(int capacity) {
		super();
		this.capacity = capacity;
		
		lockFactoryLock = new ReentrantLock();
		lockPool = new LinkedList<ReentrantLock>(); // MpmcArrayQueue (based on a ConcurrentCircularArrayQueue) from JCTools
	}

	/**
     * Acquires a lock from the pool or creates a new one if necessary.
     */
	public ReentrantLock aquire() {
		ReentrantLock result = null;
		
		lockFactoryLock.lock();
		try {
			result = lockPool.poll();
		} finally {
			lockFactoryLock.unlock();
		}

		if (result == null)
			 result = new ReentrantLock();
		result.lock();

		return result;
	}

	/**
     * Releases the lock and returns it to the pool if there is capacity.
     */
	public void release(ReentrantLock lock) {
		lock.unlock();
		
		lockFactoryLock.lock();
		try {
			if (lockPool.size() < capacity)
				lockPool.offer(lock);
		} finally {
			lockFactoryLock.unlock();
		}
	}
}
