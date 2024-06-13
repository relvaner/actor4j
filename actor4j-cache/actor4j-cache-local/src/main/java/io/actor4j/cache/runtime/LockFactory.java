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
 */
//Changed implementation
public final class LockFactory {
	private final int capacity;
	private final ReentrantLock lockFactoryLock;
	private final Queue<ReentrantLock> sharedLocks;
	
	public LockFactory(int capacity) {
		super();
		this.capacity = capacity;
		
		lockFactoryLock = new ReentrantLock();
		sharedLocks = new LinkedList<ReentrantLock>(); // MpmcArrayQueue (based on a ConcurrentCircularArrayQueue) from JCTools
	}

	public ReentrantLock aquire() {
		ReentrantLock result = null;
		
		lockFactoryLock.lock();
		try {
			if (!sharedLocks.isEmpty())
				result = sharedLocks.poll();
		} finally {
			lockFactoryLock.unlock();
		}

		result = result != null ? result : new ReentrantLock();
		result.lock();

		return result;
	}

	public void release(ReentrantLock lock) {
		lock.unlock();
		
		lockFactoryLock.lock();
		try {
			if (sharedLocks.size() < capacity)
				sharedLocks.offer(lock);
		} finally {
			lockFactoryLock.unlock();
		}
	}
}
