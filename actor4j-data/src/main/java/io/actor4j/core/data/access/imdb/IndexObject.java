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
package io.actor4j.core.data.access.imdb;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
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

public class IndexObject<K, V> {
	public String name;
	
	public Map<Object, Set<K>> map;
	// for creating the map
	public Function<Map<K, V>, Map<Object, Set<K>>> create;
	// returns values of data for the given index key
	public BiFunction<Map<K, V>, Object, Stream<V>> get;
	// replace index key of map
	public Consumer<Entry<Object, Set<K>>> set;
	// removes index key of map
	public Function<Object, Set<K>> remove;
	// reduce operation
	public Supplier<Optional<Object>> reduce;
	// add new entry of data for the index
	public BiConsumer<K, V> setd;
	// removes entry of data of the index
	public BiConsumer<K, V> removed;
	 
	public IndexObject(String name) {
		this.name = name;
	}

	public IndexObject<K, V> create(BiFunction<K, V, Object> function) {
		if (function!=null) {
			create = (data) -> { 
				Map<Object, Set<K>> map = new TreeMap<>();
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
	
	public IndexObject<K, V> get() {
		get = (data, key) -> map.get(key).stream().map((k) -> data.get(k));
		
		return this;
	}
	
	public IndexObject<K, V> get(Function<K, V> function) {
		get = (data, key) -> map.get(key).stream().map((k) -> function.apply(k));
		
		return this;
	}
	
	public IndexObject<K, V> set() {
		set = (entry) -> map.put(entry.getKey(), entry.getValue());
		
		return this;
	}
	
	public IndexObject<K, V> remove() {
		remove = (key) -> map.remove(key);
		
		return this;
	}
	
	public IndexObject<K, V> reduce(BiFunction<Object, Set<K>, Object> function, BinaryOperator<Object> accumulator) {
		reduce = () -> map.entrySet().stream().map((entry) -> function.apply(entry.getKey(), entry.getValue())).reduce(accumulator);
		
		return this;
	}
	
	public IndexObject<K, V> setd(BiFunction<K, V, Object> function) {
		setd = (k, v) -> {
			Object key = function.apply(k, v);
			
			Set<K> set = map.get(key);
			if (set==null) {
				set = new HashSet<>();
				set.add(k);
				map.put(key, set);
			}
			else
				set.add(k);
		};
		
		return this;
	}
	
	public IndexObject<K, V> removed(BiFunction<K, V, Object> function) {
		removed = (k, v) -> {
			Object key = function.apply(k, v);
			
			Set<K> set = map.get(key);
			if (set!=null)
				set.remove(k);
		};
		
		return this;
	}
}
