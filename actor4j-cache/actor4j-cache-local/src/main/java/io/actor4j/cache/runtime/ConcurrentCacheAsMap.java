/*
 * Copyright (c) 2015-2023, David A. Bauer. All rights reserved.
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
package io.actor4j.cache.runtime;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.actor4j.cache.StorageReader;
import io.actor4j.cache.StorageWriter;
import io.actor4j.cache.ConcurrentCache;

public class ConcurrentCacheAsMap<K, V> implements ConcurrentCache<K, V> {
	private final String cacheName;
	
	private final Map<K, V> map;
	private final Set<K> cacheMiss;
	private final Set<K> cacheDirty;
	private final Set<K> cacheDel;
	
	private final LockManager<K> lockManager;
	
	private final AtomicBoolean disabled;
	private final AtomicInteger clients;
	
	private final StorageReader<K, V> storageReader;
	private final StorageWriter<K, V> storageWriter;
	
	public ConcurrentCacheAsMap(String cacheName, StorageReader<K, V> storageReader, StorageWriter<K, V> storageWriter) {
		map = new ConcurrentHashMap<>();
		cacheMiss = ConcurrentHashMap.newKeySet();
		cacheDirty = ConcurrentHashMap.newKeySet();
		cacheDel = ConcurrentHashMap.newKeySet();
		lockManager = new LockManager<>();
		disabled = new AtomicBoolean(false);
		clients = new AtomicInteger(0);
		
		this.cacheName = cacheName;
		this.storageReader = storageReader;
		this.storageWriter = storageWriter;
	}
	
	public ConcurrentCacheAsMap(String cacheName) {
		this(cacheName, null, null);
	}
	
	public Map<K, V> getMap() {
		return map;
	}
	
	@Override
	public String name() {
		return cacheName;
	}

	@Override
	public V get(K key) {
		while (disabled.get());
		
		V result = null;
		
		clients.incrementAndGet();
		lockManager.lock(key);
		try {
			result = map.get(key);
			
			if (result==null) {
				if (storageReader!=null) {
					if (!cacheMiss.contains(key) && !cacheDel.contains(key)) {
						cacheMiss.add(key);
						storageReader.get(key, (v) -> putIfAbsentLocal(key, v));
					}
				}
			}
		}
		finally {
			lockManager.unLock(key);
		}
		clients.decrementAndGet();
		
		return result;
	}
	
	private void putIfAbsentLocal(K key, V value) {
		while (disabled.get());

		clients.incrementAndGet();
		lockManager.lock(key);
		try {
			V oldValue = map.putIfAbsent(key, value);
			
			if (oldValue==null)
				cacheMiss.remove(key);
		}
		finally {
			lockManager.unLock(key);
		}
		clients.decrementAndGet();
	}
	
	@Override
	public V put(K key, V value) {
		while (disabled.get());
		
		V result = null;
		
		clients.incrementAndGet();
		lockManager.lock(key);
		try {
			result = map.put(key, value);
			
			if (storageWriter!=null) {
				if (cacheDel.contains(key))
					cacheDel.remove(key);
				cacheDirty.add(key);
				storageWriter.put(key, value, () -> removeDirty(key, value));
			}
		}
		finally {
			lockManager.unLock(key);
		}
		clients.decrementAndGet();
		
		return result;
	}
	
	private void removeDirty(K key, V value) {
		while (disabled.get());

		clients.incrementAndGet();
		lockManager.lock(key);
		try {
			if (cacheDirty.contains(key)) {
				V current = map.get(key);
				if (current!=null && current.equals(value))
					cacheDirty.remove(key);
			}
		}
		finally {
			lockManager.unLock(key);
		}
		clients.decrementAndGet();
	}
	
	@Override
	public void remove(K key) {
		while (disabled.get());
		
		clients.incrementAndGet();
		lockManager.lock(key);
		try {
			map.remove(key);
			
			if (storageWriter!=null)
				if (!cacheDel.contains(key)) {
					if (cacheDirty.contains(key))
						cacheDirty.remove(key);
					cacheDel.add(key);
					storageWriter.remove(key, () -> removeIfDelLocal(key));
				}
		}
		finally {
			lockManager.unLock(key);
		}
		clients.decrementAndGet();
	}
	
	private void removeIfDelLocal(K key) {
		while (disabled.get());

		clients.incrementAndGet();
		lockManager.lock(key);
		try {
			if (cacheDel.contains(key))
				cacheDel.remove(key);
		}
		finally {
			lockManager.unLock(key);
		}
		clients.decrementAndGet();
	}
	
	@Override
	public void clear() {
		while (!disabled.compareAndSet(false, true));
		
		while (clients.get()>0);
		map.clear();
		cacheMiss.clear();
		cacheDirty.clear();
		cacheDel.clear();

		disabled.set(false);
	}
	
	@Override
	public void evict(long duration) {
		// empty
	}
	
	@Override
	public void synchronizeWithStorage() {
		while (disabled.get());

		clients.incrementAndGet();
		for (K key : cacheDirty) {
			lockManager.lock(key);
			try {
				V value = map.get(key);
				storageWriter.put(key, value, () -> removeDirty(key, value));
			}
			finally {
				lockManager.unLock(key);
			}
		}
		for (K key : cacheDel) {
			lockManager.lock(key);
			try {
				storageWriter.remove(key, () -> removeIfDelLocal(key));
			}
			finally {
				lockManager.unLock(key);
			}
		}
		clients.decrementAndGet();
	}

	@Override
	public String toString() {
		return "ConcurrentCacheAsMap [map=" + map + "]";
	}
}
