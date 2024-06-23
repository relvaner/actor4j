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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.SortedMap;

import io.actor4j.cache.StorageReader;
import io.actor4j.cache.StorageWriter;
import io.actor4j.cache.ConcurrentCache;

public class ConcurrentCacheVolatileLRU<K, V> implements ConcurrentCache<K, V>  {
	private final String cacheName;
	
	private static record Pair<V>(V value, long timestamp) {
		public static <V> Pair<V> of(V value, long timestamp) {
			return new Pair<V>(value, timestamp);
		}
	}
	
	private final Map<K, Pair<V>> map;
	private final SortedMap<Long, K> lru;
	private final Set<K> cacheMiss;
	private final Set<K> cacheDirty;
	private final Set<K> cacheDel;
	
	private final LockManager<K> lockManager;
	private final int size;
	
	private final AtomicBoolean disabled;
	private final AtomicInteger clients;
	
	private final StorageReader<K, V> storageReader;
	private final StorageWriter<K, V> storageWriter;
	
	public ConcurrentCacheVolatileLRU(String cacheName, int size, StorageReader<K, V> storageReader, StorageWriter<K, V> storageWriter) {
		map = new ConcurrentHashMap<>(size);
		lru = new ConcurrentSkipListMap<>();
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
	
	public ConcurrentCacheVolatileLRU(String cacheName, int size) {
		this(cacheName, size, null, null);
	}
		
	public Map<K, Pair<V>> getMap() {
		return map;
	}
	
	public SortedMap<Long, K> getLru() {
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
	public V get(K key) {
		while (disabled.get());
		
		V result = null;
		
		clients.incrementAndGet();
		lockManager.lock(key);
		try {
			Pair<V> pair = map.get(key);
			
			if (pair==null) {
				if (storageReader!=null) {
					if (!cacheMiss.contains(key) && !cacheDel.contains(key)) {
						cacheMiss.add(key);
						storageReader.get(key, (v) -> putIfAbsentLocal(key, v));
					}
				}
			}
			else {
				lru.remove(pair.timestamp());
				long timestamp = System.nanoTime();
				map.put(key, new Pair<V>(pair.value(), timestamp));
				lru.put(timestamp, key);
				result = pair.value();
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
			long timestamp = System.nanoTime();
			Pair<V> oldPair = map.putIfAbsent(key, Pair.of(value, timestamp));
			
			if (oldPair==null) {
				resize();
				lru.put(timestamp, key);
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
			long timestamp = System.nanoTime();
			Pair<V> pair = map.put(key, new Pair<V>(value, timestamp));
			
			if (pair==null) {
				resize();
				lru.put(timestamp, key);
			}
			else {
				lru.remove(pair.timestamp);
				lru.put(timestamp, key);
				result = pair.value();
			}
			
			if (storageWriter!=null) {
				if (cacheDel.contains(key))
					cacheDel.remove(key);
				cacheDirty.add(key);
				storageWriter.put(key, value, () -> removeDirty(key, pair));
			}
		}
		finally {
			lockManager.unLock(key);
		}
		clients.decrementAndGet();
		
		return result;
	}
	
	private void removeDirty(K key, Pair<V> pair) {
		while (disabled.get());

		clients.incrementAndGet();
		lockManager.lock(key);
		try {
			if (cacheDirty.contains(key)) {
				Pair<V> current = map.get(key);
				if (current!=null && current.equals(pair))
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
			Pair<V> pair = map.get(key);
			lru.remove(pair.timestamp());
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
		lru.clear();
		cacheMiss.clear();
		cacheDirty.clear();
		cacheDel.clear();

		disabled.set(false);
	}
	
	private void resize() {
		if (map.size()>size) {
			long timestamp = lru.firstKey();
			map.remove(lru.get(timestamp));
			lru.remove(timestamp);
		}
	}
	
	@Override
	public void evict(long duration) {
		long currentTime = System.currentTimeMillis();
		
		Iterator<Entry<Long, K>> iterator = lru.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<Long, K> entry = iterator.next();
			if (currentTime-entry.getKey()/1_000_000>duration) {
				map.remove(entry.getValue());
				iterator.remove();
			}
		}
	}
	
	@Override
	public void synchronizeWithStorage() {
		while (disabled.get());

		clients.incrementAndGet();
		for (K key : cacheDirty) {
			lockManager.lock(key);
			try {
				Pair<V> pair = map.get(key);
				storageWriter.put(key, pair.value(), () -> removeDirty(key, pair));
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
		return "ConcurrentCacheVolatileLRU [map=" + map + ", lru=" + lru + ", size=" + size + "]";
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