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
package io.actor4j.core.data.access;

import java.util.List;
import java.util.UUID;

public record PersistentDataAccessDTO<K, V>(UUID id, boolean keyExists, K key, Object value, PersistentContext context, UUID source, Object reserved) implements PersistentDTO<K, V> {
	public PersistentDataAccessDTO(K key, V value, PersistentContext context, UUID source, Object reserved) {
		this(UUID.randomUUID(), false, key, value, context, source, reserved);
	}
	
	public PersistentDataAccessDTO(K key, V value, PersistentContext context, UUID source) {
		this(key, value, context, source, null);
	}
	
	public PersistentDataAccessDTO(K key, PersistentContext context, UUID source) {
		this(key, null, context, source, null);
	}
	
	public PersistentDataAccessDTO(PersistentContext context, UUID source) {
		this(null, null, context, source, null);
	}
	
	public PersistentDataAccessDTO(K key, V value, UUID source) {
		this(key, value, null, source, null);
	}
	
	public PersistentDataAccessDTO(K key, UUID source) {
		this(key, null, null, source, null);
	}
	
	public PersistentDataAccessDTO(UUID source) {
		this(null, null, null, source, null);
	}
	
	public PersistentDataAccessDTO<K, V> shallowCopy(boolean keyExists) {
		return new PersistentDataAccessDTO<K, V>(id, keyExists, key, value, context, source, reserved);
	}
	
	public PersistentDataAccessDTO<K, V> shallowCopyWithKey(K key) {
		// Presume keyExists=true
		return new PersistentDataAccessDTO<K, V>(id, true, key, value, context, source, reserved);
	}
	
	public PersistentDataAccessDTO<K, V> shallowCopy(V value) {
		// Presume keyExists=true
		return new PersistentDataAccessDTO<K, V>(id, true, key, value, context, source, reserved);
	}
	
	public PersistentDataAccessDTO<K, V> shallowCopyWithEntities(List<V> entities) {
		// Presume keyExists=true
		return new PersistentDataAccessDTO<K, V>(id, true, key, entities, context, source, reserved);
	}
	
	public PersistentDataAccessDTO<K, V> shallowCopy(K key, V value) {
		// Presume keyExists=true
		return new PersistentDataAccessDTO<K, V>(id, true, key, value, context, source, reserved);
	}
	
	public PersistentDataAccessDTO<K, V> shallowCopyWithReserved(Object reserved) {
		return new PersistentDataAccessDTO<K, V>(id, keyExists, key, value, context, source, reserved);
	}
	
	public PersistentDataAccessDTO<K, V> shallowCopy(UUID source) {
		return new PersistentDataAccessDTO<K, V>(id, keyExists, key, value, context, source, reserved);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<V> valueAsList() {
		return (List<V>)value;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public V entity() {
		return (V)value;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<V> entities() {
		return (List<V>)value;
	}
}
