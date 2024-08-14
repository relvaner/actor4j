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

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.actor4j.cache.StorageReader;
import io.actor4j.cache.StorageWriter;
import io.actor4j.cache.ConcurrentCache;

public class ConcurrentCacheLRU<K, V> implements ConcurrentCache<K, V> {
	private final String cacheName;
	
	private final Map<K, V> map;
	private final Deque<K> lru;
	private final Set<K> cacheMiss;
	private final Set<K> cacheDirty;
	private final Set<K> cacheDel;
	
	private final LockManager<K> lockManager;
	private final int size;
	
	private final AtomicBoolean disabled;
	private final AtomicInteger clients;
	
	private final StorageReader<K, V> storageReader;
	private final StorageWriter<K, V> storageWriter;
	
	public ConcurrentCacheLRU(String cacheName, int size, StorageReader<K, V> storageReader, StorageWriter<K, V> storageWriter) {
		super();
		
		map = new ConcurrentHashMap<>(size);
		lru = new ConcurrentLinkedDeque<>();
		cacheMiss = ConcurrentHashMap.newKeySet();
		cacheDirty = ConcurrentHashMap.newKeySet();
		cacheDel = ConcurrentHashMap.newKeySet();
		lockManager = new LockManager<>();
		disabled = new AtomicBoolean(false);
		clients = new AtomicInteger(0);
		
		this.cacheName = cacheName;
		this.size = size;
		this.storageReader = storageReader;
		this.storageWriter = storageWriter;
	}
	
	public ConcurrentCacheLRU(String cacheName, int size) {
		this(cacheName, size, null, null);
	}
	
	public Map<K, V> getMap() {
		return map;
	}

	public Deque<K> getLru() {
		return lru;
	}
	
	@Override
	public String name() {
		return cacheName;
	}
	
	public int size() {
		return size;
	}
	
	@Override
	public boolean containsKey(K key) {
		while (disabled.get());
		
		boolean result = false;
		
		clients.incrementAndGet();
		lockManager.lock(key);
		try {
			result = map.containsKey(key);
		}
		finally {
			lockManager.unLock(key);
		}
		clients.decrementAndGet();
		
		return result;
	}
	
	@Override
	public V get(K key) {
		while (disabled.get());
		
		V result = null;
		
		clients.incrementAndGet();
		lockManager.lock(key);
		try {
			result = map.get(key);
			
			if (result==null && !map.containsKey(key)) {
				if (storageReader!=null) {
					if (!cacheMiss.contains(key) && !cacheDel.contains(key)) {
						cacheMiss.add(key);
						storageReader.get(key, (v) -> putIfAbsentLocal(key, v));
					}
				}
			}
			else {
				lru.remove(key);
				lru.addLast(key);
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
			
			if (oldValue==null) {
				resize();
				lru.addLast(key);
				cacheMiss.remove(key);
			}
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
			
			if (result==null) {
				resize();
				lru.addLast(key);
			}
			else {
				lru.remove(key);
				lru.addLast(key);
			}
			
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
	public void writeAround(K key, V value) {
		if (storageWriter!=null)
			storageWriter.put(key, value, null);
	}
	
	// Works only if already available in the cache and if the value is non-null
	@Override
	public boolean compareAndSet(K key, V expectedValue, V newValue) {
		boolean result = false;
		
		while (disabled.get());

		clients.incrementAndGet();
		lockManager.lock(key);
		try {
			V value = map.get(key);
			if (value!=null && value.equals(expectedValue)) {
				map.put(key, newValue);
				
				lru.remove(key);
				lru.addLast(key);
				
				if (storageWriter!=null) {
					if (cacheDel.contains(key))
						cacheDel.remove(key);
					cacheDirty.add(key);
					storageWriter.put(key, newValue, () -> removeDirty(key, newValue));
				}
			}
		}
		finally {
			lockManager.unLock(key);
		}
		clients.decrementAndGet();

		return result;
	}
	
	@Override
	public void remove(K key) {
		while (disabled.get());
		
		clients.incrementAndGet();
		lockManager.lock(key);
		try {
			map.remove(key);
			lru.remove(key);
			
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
		lru.clear();
		cacheMiss.clear();
		cacheDirty.clear();
		cacheDel.clear();

		disabled.set(false);
	}
	
	private void resize() {
		if (map.size()>size) {
			map.remove(lru.getFirst());
			lru.removeFirst();
		}
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
		return "ConcurrentCacheLRU [map=" + map + ", lru=" + lru + ", size=" + size + "]";
	}

	@Override
	public Map<K, V> get(List<K> keys) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void put(Map<K, V> entries) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void remove(List<K> keys) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void close() {
		// empty
	}
}
