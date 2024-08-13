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
package io.actor4j.core.data.access.cache;

import static io.actor4j.core.actors.ActorWithCache.UPDATE;
import static io.actor4j.core.data.access.DataAccessActor.INSERT_ONE;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.actor4j.core.actors.ActorWithCache;
import io.actor4j.core.data.access.DataAccessActor;
import io.actor4j.core.utils.ActorOptional;

import java.util.SortedMap;
import java.util.TreeMap;

public class AsyncCacheVolatileLRU<K, V> implements AsyncCache<K, V>  {
	protected static record Pair<V>(V value, long timestamp) {
		public static <V> Pair<V> of(V value, long timestamp) {
			return new Pair<V>(value, timestamp);
		}
	}
	
	protected final Map<K, Pair<V>> map;
	protected final SortedMap<Long, K> lru;
	protected final Set<K> cacheMiss;
	protected final Set<K> cacheDirty;
	protected final Set<K> cacheDel;
	
	protected final int size;

	public AsyncCacheVolatileLRU(int size) {
		super();
		
		map = new HashMap<>(size);
		lru = new TreeMap<>();
		cacheMiss = new HashSet<>();
		cacheDirty = new HashSet<>();
		cacheDel = new HashSet<>();
		
		this.size = size;
	}
		
	public Map<K, Pair<V>> getMap() {
		return map;
	}
	
	public SortedMap<Long, K> getLru() {
		return lru;
	}
	
	public int size() {
		return size;
	}

	@Override
	public boolean containsKey(K key) {
		return map.containsKey(key);
	}
	
	@Deprecated
	@Override
	public V get(K key) {
		return null;
	}
	
	@Override
	public ActorOptional<V> get(K key, Runnable storageReader, Runnable cacheMissFlaggedHandler, Runnable cacheDelFlaggedHandler) {
		Pair<V> pair = map.get(key);
		
		if (pair==null) {
			if (!cacheMiss.contains(key) && !cacheDel.contains(key)) {
				cacheMiss.add(key);
				storageReader.run();
			}
			else if (cacheDel.contains(key))
				cacheDelFlaggedHandler.run();
			else if (cacheMiss.contains(key))
				cacheMissFlaggedHandler.run();
			
			return ActorOptional.none();
		}
		else {
			lru.remove(pair.timestamp());
			long timestamp = System.nanoTime();
			map.put(key, new Pair<V>(pair.value(), timestamp));
			lru.put(timestamp, key);
			
			return ActorOptional.of(pair.value());
		}
	}
	
	protected void putIfAbsentLocal(K key, V value) {
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
		
		if (cacheDel.contains(key))
			cacheDel.remove(key);
		cacheDirty.add(key);
		
		return result;
	}
	
	protected void removeDirty(K key, V value/*Pair<V> pair*/) {
		if (cacheDirty.contains(key)) {
			Pair<V> pair = map.get(key);
			if (pair!=null) {
				V currentValue = pair.value();
				if (currentValue.equals(value))
					cacheDirty.remove(key);
			}
		}
	}
	
	@Deprecated
	@Override
	public void remove(K key) {
		// empty
	}
	
	@Override
	public void remove(K key, Runnable storageWriter, Runnable cacheDelFlaggedHandler) {
		Pair<V> pair = map.get(key);
		lru.remove(pair.timestamp);
		map.remove(key);
		
		
		if (!cacheDel.contains(key)) {
			if (cacheDirty.contains(key))
				cacheDirty.remove(key);
			cacheDel.add(key);
			storageWriter.run();
		}
		else
			cacheDelFlaggedHandler.run();
	}
	
	protected void removeIfDelLocal(K key) {
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
	public String toString() {
		return "CacheLRUWithGC [map=" + map + ", lru=" + lru + ", size=" + size + "]";
	}
	
	@Override
	public void complete(int tag, K key, V value) {
		if (tag==ActorWithCache.GET || tag==DataAccessActor.FIND_ONE)
			putIfAbsentLocal(key, value);
		else if (tag==ActorWithCache.SET || tag==INSERT_ONE)
			removeDirty(key, value);
		else if (tag==DataAccessActor.DELETE_ONE || tag==DataAccessActor.UPDATE_ONE || tag==UPDATE)
			removeIfDelLocal(key);
	}
	
	@Override
	public void synchronizeWithStorage(BiConsumer<K, V> cacheDirtyFlaggedHandler, Consumer<K> cacheDelFlaggedHandler) {
		for (K key : cacheDirty) {
			Pair<V> pair = map.get(key);
			if (pair!=null)
				cacheDirtyFlaggedHandler.accept(key, pair.value());
		}
		
		for (K key : cacheDel)
			cacheDelFlaggedHandler.accept(key);
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
	public boolean compareAndSet(K key, V expectedValue, V newValue) {
		return false;
	}
	
	@Override
	public void close() {
		// empty
	}
}