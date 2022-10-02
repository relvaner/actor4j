/*
 * Copyright (c) 2015-2022, David A. Bauer. All rights reserved.
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
package io.actor4j.core.data.access;

import java.util.UUID;

public interface VolatileDTO<K, V> {
	public K key();
	public V value();
	public UUID source();

	public static <K, V> VolatileDataAccessDTO<K, V> create() {
		return new VolatileDataAccessDTO<K, V>();
	}
	
	public static <K, V> VolatileDataAccessDTO<K, V> create(K key) {
		return new VolatileDataAccessDTO<K, V>(key);
	}
	
	public static <K, V> VolatileDataAccessDTO<K, V> create(K key, V value) {
		return new VolatileDataAccessDTO<K, V>(key, value);
	}
	
	public static <K, V> VolatileDataAccessDTO<K, V> create(K key, UUID source) {
		return new VolatileDataAccessDTO<K, V>(key, source);
	}
}
