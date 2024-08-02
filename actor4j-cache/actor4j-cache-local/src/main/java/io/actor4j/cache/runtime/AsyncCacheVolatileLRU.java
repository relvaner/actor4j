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
package io.actor4j.cache.runtime;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import io.actor4j.cache.AsyncCache;
import io.actor4j.cache.StorageReader;
import io.actor4j.cache.StorageWriter;

import java.util.SortedMap;
import java.util.TreeMap;

public class AsyncCacheVolatileLRU<K, V> implements AsyncCache<K, V>  {
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
	
	private final int size;
	
	private final StorageReader<K, V> storageReader;
	private final StorageWriter<K, V> storageWriter;

	public AsyncCacheVolatileLRU(int size) {
		this(size, null, null);
	}
	
	public AsyncCacheVolatileLRU(int size, StorageReader<K, V> storageReader, StorageWriter<K, V> storageWriter) {
		map = new HashMap<>(size);
		lru = new TreeMap<>();
		cacheMiss = new HashSet<>();
		cacheDirty = new HashSet<>();
		cacheDel = new HashSet<>();
		
		this.size = size;
		this.storageReader = storageReader;
		this.storageWriter = storageWriter;
	}
		
	public Map<K, Pair<V>> getMap() {
		return map;
	}
	
	public SortedMap<Long, K> getLru() {
		return lru;
	}

	@Override
	public V get(K key) {
		V result = null;
		
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
		
		return result;
	}
	
	private void putIfAbsentLocal(K key, V value) {
		long timestamp = System.nanoTime();
		Pair<V> oldPair = map.putIfAbsent(key, Pair.of(value, timestamp));
		
		if (oldPair==null) {
			resize();
			lru.put(timestamp, key);
			cacheMiss.remove(key);
		}
	}
	
	@Override
	public V put(K key, V value) {
		V result = null;
		
		long timestamp = System.nanoTime();
		Pair<V> pair = map.put(key, new Pair<V>(value, timestamp));
		
		if (pair==null) {
			resize();
			lru.put(timestamp, key);
		}
		else {
			lru.remove(pair.timestamp);
			lru.put(timestamp, key);
			result = pair.value;
		}
		
		if (storageWriter!=null) {
			if (cacheDel.contains(key))
				cacheDel.remove(key);
			cacheDirty.add(key);
			storageWriter.put(key, value, () -> removeDirty(key, pair));
		}
		
		return result;
	}
	
	private void removeDirty(K key, Pair<V> pair) {
		if (cacheDirty.contains(key)) {
			Pair<V> current = map.get(key);
			if (current!=null && current.equals(pair))
				cacheDirty.remove(key);
		}
	}
	
	@Override
	public void remove(K key) {
		Pair<V> pair = map.get(key);
		lru.remove(pair.timestamp);
		map.remove(key);
		
		if (storageWriter!=null)
			if (!cacheDel.contains(key)) {
				if (cacheDirty.contains(key))
					cacheDirty.remove(key);
				cacheDel.add(key);
				storageWriter.remove(key, () -> removeIfDelLocal(key));
			}
	}
	
	private void removeIfDelLocal(K key) {
		if (cacheDel.contains(key))
			cacheDel.remove(key);
	}
	
	@Override
	public void clear() {
		map.clear();
		lru.clear();
		cacheMiss.clear();
		cacheDirty.clear();
		cacheDel.clear();
	}
	
	protected void resize() {
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
		for (K key : cacheDirty) {
			Pair<V> pair = map.get(key);
			storageWriter.put(key, pair.value(), () -> removeDirty(key, pair));
		}
		for (K key : cacheDel)
			storageWriter.remove(key, () -> removeIfDelLocal(key));
	}
	
	@Override
	public String toString() {
		return "CacheLRUWithGC [map=" + map + ", lru=" + lru + ", size=" + size + "]";
	}
	
	@Override
	public Map<K, V> get(List<K> keys) {
		return null;
	}

	@Override
	public void put(Map<K, V> entries) {
		// empty
	}

	@Override
	public void remove(List<K> keys) {
		// empty
	}
	
	@Override
	public void close() {
		// empty
	}
}