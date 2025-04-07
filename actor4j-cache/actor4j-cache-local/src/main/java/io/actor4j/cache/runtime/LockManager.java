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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A mechanism to manage locks for a collection of objects.
 *
 * @param <K> the type of the object to be locked
 * @author Yannis Cosmadopoulos
 * @author Greg Luck
 */
// Changed implementation
public final class LockManager<K> {
	private final ConcurrentHashMap<K, ReentrantLock> locks;
	
	private static final LockFactory lockFactory;
	
	static {
		lockFactory = new LockFactory(Runtime.getRuntime().availableProcessors()*2/**factor*/);
	}

	/**
	 * Constructor
	 */
	public LockManager() {
		super();

		locks = new ConcurrentHashMap<K, ReentrantLock>();
	}

	/**
	 * Lock the object
	 *
	 * @param key the key
	 */
	public void lock(K key) {
		ReentrantLock newLock  = lockFactory.aquire();

		while (true) {
			ReentrantLock existingLock = locks.putIfAbsent(key, newLock);
			if (existingLock == null)
				return;

			// A lock already associated with the given key, we have to wait until release
			existingLock.lock();
			// Now we have it, we will release the lock so its potentially available for reuse (see LockFactory, capacity)
			existingLock.unlock();
		}
	}

	/**
	 * Unlock the object
	 *
	 * @param key the object
	 */
	public void unLock(K key) {
		ReentrantLock lock = locks.remove(key);
		
		if (lock != null)
			lockFactory.release(lock);
	}
}
