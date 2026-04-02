/*
 * Copyright (c) 2015-2018, David A. Bauer. All rights reserved.
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
package io.actor4j.core.data.access.ims;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class IMSIndex<K, V> {
	public final String name;
	
	public Map<Object, Set<K>> idxMap;
	
	/**
	 * General Index Operations
	 */
	// for creating the map
	public Function<Map<K, V>, Map<Object, Set<K>>> idxCreate;
	// returns values of data for the given index key
	public BiFunction<Map<K, V>, Object, Stream<V>> idxGet;
	// replace index key of map
	public Consumer<Entry<Object, Set<K>>> idxSet;
	// removes index key of map
	public Function<Object, Set<K>> idxRemove;
	// reduce operation
	public Supplier<Optional<Object>> idxReduce;
	
	/**
	 * Data Sync Operations
	 */
	// add new entry of data for the index
	public BiConsumer<K, V> insertToIdx;
	// removes entry of data of the index
	public BiConsumer<K, V> removeFromIdx;
	 
	public IMSIndex(String name) {
		this.name = name;
	}
	
	public IMSIndex<K, V> create(BiFunction<K, V, Object> function) {
		return create(function, false);
	}

	public IMSIndex<K, V> create(BiFunction<K, V, Object> function, boolean sorted) {
		if (function!=null) {
			idxCreate = (data) -> { 
				Map<Object, Set<K>> map = sorted ? new TreeMap<>() : new HashMap<>();
				
				Iterator<Entry<K, V>> iterator = data.entrySet().iterator();
				while (iterator.hasNext()) {
					Entry<K, V> entry = iterator.next();
					
					Object key = function.apply(entry.getKey(), entry.getValue());
					
					Set<K> set = map.get(key);
					if (set==null) {
						set = new HashSet<>();
						set.add(entry.getKey());
						map.put(key, set);
					}
					else
						set.add(entry.getKey());
				}
				return map;
			};
		}
		
		return this;
	}
	
	public SortedMap<Object, Set<K>> subMap(Object fromKey, Object toKey) {
		SortedMap<Object, Set<K>> result = null;
		if (idxMap instanceof TreeMap)
			result =((TreeMap<Object, Set<K>>) idxMap).subMap(fromKey, toKey);
		
		return result;
	}
	
	public IMSIndex<K, V> get() {
		idxGet = (data, key) -> idxMap.getOrDefault(key, Collections.emptySet()).stream().map(data::get);
		
		return this;
	}
	
	public IMSIndex<K, V> get(BiFunction<Map<K, V>, K, V> function) {
		idxGet = (data, key) -> idxMap.getOrDefault(key, Collections.emptySet()).stream().map((k) -> function.apply(data, k));
		
		return this;
	}
	
	public IMSIndex<K, V> set() {
		idxSet = (entry) -> idxMap.put(entry.getKey(), entry.getValue());
		
		return this;
	}
	
	public IMSIndex<K, V> remove() {
		idxRemove = (key) -> idxMap.remove(key);
		
		return this;
	}
	
	public IMSIndex<K, V> reduce(BiFunction<Object, Set<K>, Object> function, BinaryOperator<Object> accumulator) {
		idxReduce = () -> idxMap.entrySet().stream().map((entry) -> function.apply(entry.getKey(), entry.getValue())).reduce(accumulator);
		
		return this;
	}
	
	public IMSIndex<K, V> insertData(BiFunction<K, V, Object> function) {
		insertToIdx = (k, v) -> {
			Object key = function.apply(k, v);
			
			Set<K> set = idxMap.get(key);
			if (set==null) {
				set = new HashSet<>();
				set.add(k);
				idxMap.put(key, set);
			}
			else
				set.add(k);
		};
		
		return this;
	}
	
	public IMSIndex<K, V> removeData(BiFunction<K, V, Object> function) {
		removeFromIdx = (k, v) -> {
			Object key = function.apply(k, v);
			
			Set<K> set = idxMap.get(key);
			if (set!=null)
				set.remove(k);
		};
		
		return this;
	}
	
	public IMSIndex<K, V> syncData(BiFunction<K, V, Object> function) {
		return insertData(function).removeData(function);
	}
}
