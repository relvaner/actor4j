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
package io.actor4j.cache.utils;

import javax.cache.Cache.Entry;

public record DummyCacheEntry<K, V>(K key, V value) implements Entry<K, V> {
	public static <K, V> Entry<K, V> create(K key, V value) {
		return new DummyCacheEntry<K, V>(key, value);
	}
	
	@Override
	public K getKey() {
		return key;
	}

	@Override
	public V getValue() {
		return value;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T unwrap(Class<T> clazz) {
		T result = null; 
		
		if (clazz.equals(this.getClass()))
			result = (T)this;
		else
			throw new IllegalArgumentException();
		
		return result;
	}
}
