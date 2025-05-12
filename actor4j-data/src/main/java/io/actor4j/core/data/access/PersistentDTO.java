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

import io.actor4j.core.id.ActorId;

public interface PersistentDTO<K, V> extends VolatileDTO<K, V> {
	public PersistentContext context();
	
	public static <K, V> PersistentDataAccessDTO<K, V> create(K key, ActorId source) {
		return new PersistentDataAccessDTO<K, V>(key, source);
	}
	
	public static <K, V> PersistentDataAccessDTO<K, V> create(K key, PersistentContext context, ActorId source) {
		return new PersistentDataAccessDTO<K, V>(key, context, source);
	}
	
	public static <K, V> PersistentDataAccessDTO<K, V> create(K key, V value, ActorId source) {
		return new PersistentDataAccessDTO<K, V>(key, value, source);
	}
	
	public static <K, V> PersistentDataAccessDTO<K, V> create(K key, V value, PersistentContext context, ActorId source) {
		return new PersistentDataAccessDTO<K, V>(key, value, context, source);
	}
	
	public static <K, V> PersistentDataAccessDTO<K, V> create(K key, V value, int hashCodeExpected, ActorId source) {
		return new PersistentDataAccessDTO<K, V>(key, value, hashCodeExpected, null, source, false);
	}
	
	public static <K, V> PersistentDataAccessDTO<K, V> create(K key, V value, int hashCodeExpected,PersistentContext context, ActorId source) {
		return new PersistentDataAccessDTO<K, V>(key, value, hashCodeExpected, context, source, false);
	}
	
	public static <K, V> PersistentDataAccessDTO<K, V> create(K key, V value, ActorId source, boolean cacheHit) {
		return new PersistentDataAccessDTO<K, V>(key, value, 0, null, source, cacheHit);
	}
	
	public static <K, V> PersistentDataAccessDTO<K, V> create(K key, V value, PersistentContext context, ActorId source, boolean cacheHit) {
		return new PersistentDataAccessDTO<K, V>(key, value, 0, context, source, cacheHit);
	}
}
