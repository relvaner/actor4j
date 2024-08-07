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
package io.actor4j.core.data.access.cache;

import static io.actor4j.core.actors.ActorWithCache.UPDATE;
import static io.actor4j.core.data.access.DataAccessActor.INSERT_ONE;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.actor4j.core.actors.ActorWithCache;
import io.actor4j.core.data.access.DataAccessActor;
import io.actor4j.core.utils.ActorOptional;

public class AsyncCacheLRU<K, V> implements AsyncCache<K, V> {
	protected final Map<K, V> map;
	protected final Deque<K> lru;
	protected final Set<K> cacheMiss;
	protected final Set<K> cacheDirty;
	protected final Set<K> cacheDel;

	protected final int size;

	public AsyncCacheLRU(int size) {
		super();
		
		map = new HashMap<>(size);
		lru = new ArrayDeque<>(size);
		cacheMiss = new HashSet<>();
		cacheDirty = new HashSet<>();
		cacheDel = new HashSet<>();
		
		this.size = size;
	}
	
	public Map<K, V> getMap() {
		return map;
	}

	public Deque<K> getLru() {
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
		V value = map.get(key);
		
		if (value==null && !map.containsKey(key)) {
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
			lru.remove(key);
			lru.addLast(key);
			
			return ActorOptional.of(value);
		}
	}
	
	protected void putIfAbsentLocal(K key, V value) {
		V oldValue = map.putIfAbsent(key, value);
		
		if (oldValue==null) {
			resize();
			lru.addLast(key);
			cacheMiss.remove(key);
		}
	}
	
	@Override
	public V put(K key, V value) {
		V result = null;
		
		result = map.put(key, value);
		
		if (result==null) {
			resize();
			lru.addLast(key);
		}
		else {
			lru.remove(key);
			lru.addLast(key);
		}
		
		
		if (cacheDel.contains(key))
			cacheDel.remove(key);
		cacheDirty.add(key);
		
		return result;
	}
	
	protected void removeDirty(K key, V value) {
		if (cacheDirty.contains(key)) {
			V current = map.get(key);
			if (current!=null && current.equals(value))
				cacheDirty.remove(key);
		}
	}
	
	// used with update
	@Override
	public void remove(K key) {
		map.remove(key);
		lru.remove(key);

		if (!cacheDel.contains(key)) {
			if (cacheDirty.contains(key))
				cacheDirty.remove(key);
			cacheDel.add(key);
		}
	}
	
	@Override
	public void remove(K key, Runnable storageWriter, Runnable cacheDelFlaggedHandler) {
		map.remove(key);
		lru.remove(key);

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
			map.remove(lru.getFirst());
			lru.removeFirst();
		}
	}
	
	@Override
	public void evict(long duration) {
		// empty
	}

	@Override
	public String toString() {
		return "ConcurrentCacheLRU [map=" + map + ", lru=" + lru + ", size=" + size + "]";
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
			V value = map.get(key);
			cacheDirtyFlaggedHandler.accept(key, value);
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
	public void close() {
		// empty
	}
}
