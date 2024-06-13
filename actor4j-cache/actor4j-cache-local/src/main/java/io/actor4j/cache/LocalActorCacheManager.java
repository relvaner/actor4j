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
package io.actor4j.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import io.actor4j.cache.runtime.ConcurrentCacheAsMap;
import io.actor4j.cache.runtime.ConcurrentCacheLRU;
import io.actor4j.cache.runtime.ConcurrentCacheVolatileLRU;

public final class LocalActorCacheManager {
	private static final Object lock = new Object();
	private static final Map<String, ConcurrentCache<?, ?>> localCachesMap;
	
	static {
		localCachesMap = new ConcurrentHashMap<>();
	}
	
	@SuppressWarnings("unchecked")
	public static <K, V> ConcurrentCache<K, V> createCache(String cacheName, Function<String, ConcurrentCache<K, V>> factory) {
		ConcurrentCache<?, ?> result = localCachesMap.get(cacheName);
		boolean created = false;
		
		if (result==null) {
			synchronized(lock) {
				if (result==null) {
					result = factory.apply(cacheName);
					localCachesMap.put(cacheName, result);
					created = true;
				}	
			}
		}
		
		return created ? (ConcurrentCacheAsMap<K, V>)result : null;
	}
	
	@SuppressWarnings("unchecked")
	public static <K, V> ConcurrentCache<K, V> getCache(String cacheName) {
		ConcurrentCache<?, ?> result = localCachesMap.get(cacheName);
		
		return (ConcurrentCacheAsMap<K, V>)result;
	}
	
	@SuppressWarnings("unchecked")
	public static <K, V> ConcurrentCache<K, V> removeCache(String cacheName) {
		ConcurrentCache<?, ?> result = localCachesMap.remove(cacheName);
		
		return (ConcurrentCacheAsMap<K, V>)result;
	}
	
	public static <K, V> ConcurrentCache<K, V> createCacheAsMap(String cacheName) {
		return createCache(cacheName, (name) -> new ConcurrentCacheAsMap<>(name));
	}
	
	public static <K, V> ConcurrentCache<K, V> createCacheAsMap(String cacheName, StorageReader<K, V> reader, StorageWriter<K, V> writer) {
		return createCache(cacheName, (name) -> new ConcurrentCacheAsMap<>(name, reader, writer));
	}
	
	public static <K, V> ConcurrentCache<K, V> createLRUCache(String cacheName, int size) {
		return createCache(cacheName, (name) -> new ConcurrentCacheLRU<>(name, size));
	}
	
	public static <K, V> ConcurrentCache<K, V> createLRUCache(String cacheName, int size, StorageReader<K, V> reader, StorageWriter<K, V> writer) {
		return createCache(cacheName, (name) -> new ConcurrentCacheLRU<>(name, size, reader, writer));
	}
	
	public static <K, V> ConcurrentCache<K, V> createVolatileLRUCache(String cacheName, int size) {
		return createCache(cacheName, (name) -> new ConcurrentCacheVolatileLRU<>(name, size));
	}
	
	public static <K, V> ConcurrentCache<K, V> createVolatileLRUCache(String cacheName, int size, StorageReader<K, V> reader, StorageWriter<K, V> writer) {
		return createCache(cacheName, (name) -> new ConcurrentCacheVolatileLRU<>(name, size, reader, writer));
	}
}
