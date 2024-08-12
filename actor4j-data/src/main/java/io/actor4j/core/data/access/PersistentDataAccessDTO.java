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

import io.actor4j.core.json.JsonObject;

public record PersistentDataAccessDTO<K, V>(UUID id, boolean keyExists, K key, Object value, int hashCodeExpected, String query, JsonObject filter, JsonObject update, String collectionName, UUID source, Object reserved) implements PersistentDTO<K, V> {
	public PersistentDataAccessDTO(K key, V value, int hashCodeExpected, String query, JsonObject filter, JsonObject update, String collectionName, UUID source, Object reserved) {
		this(UUID.randomUUID(), false, key, value, hashCodeExpected, query, filter, update, collectionName, source, reserved);
	}
	
	public PersistentDataAccessDTO(K key, V value, String keyname, String collectionName, UUID source) {
		this(key, value, 0, null, JsonObject.create().put(keyname, key), null, collectionName, source, null);
	}
	
	public PersistentDataAccessDTO(K key, String keyname, String collectionName, UUID source) {
		this(key, null, 0, null, JsonObject.create().put(keyname, key), null, collectionName, source, null);
	}
	
	public PersistentDataAccessDTO(K key, V value, String keyname, JsonObject update, String collectionName, UUID source) {
		this(key, value, 0, null, keyname!=null ? JsonObject.create().put(keyname, key) : null, update, collectionName, source, null);
	}
	
	public PersistentDataAccessDTO(K key, V value, JsonObject filter, JsonObject update, String collectionName, UUID source) {
		this(key, value, 0, null, filter, update, collectionName, source, null);
	}
	
	public PersistentDataAccessDTO(K key, V value, String collectionName, UUID source) {
		this(key, value, 0, null, null, null, collectionName, source, null);
	}
	
	public PersistentDataAccessDTO(K key, String collectionName, UUID source) {
		this(key, null, 0, null, null, null, collectionName, source, null);
	}
	
	public PersistentDataAccessDTO(String query, UUID source) {
		this(null, null, 0, query, null, null, null, source, null);
	}
	
	public PersistentDataAccessDTO(K key, V value, UUID source) {
		this(key, value, 0, null, null, null, null, source, null);
	}
	
	public PersistentDataAccessDTO(K key, UUID source) {
		this(key, null, 0, null, null, null, null, source, null);
	}
	
	public PersistentDataAccessDTO<K, V> shallowCopy(boolean keyExists) {
		return new PersistentDataAccessDTO<K, V>(id, keyExists, key, value, hashCodeExpected, query, filter, update, collectionName, source, reserved);
	}
	
	public PersistentDataAccessDTO<K, V> shallowCopyWithKey(K key) {
		// Presume keyExists=true
		return new PersistentDataAccessDTO<K, V>(id, true, key, value, hashCodeExpected, query, filter, update, collectionName, source, reserved);
	}
	
	public PersistentDataAccessDTO<K, V> shallowCopy(V value) {
		// Presume keyExists=true
		return new PersistentDataAccessDTO<K, V>(id, true, key, value, hashCodeExpected, query, filter, update, collectionName, source, reserved);
	}
	
	public PersistentDataAccessDTO<K, V> shallowCopyWithEntities(List<V> entities) {
		// Presume keyExists=true
		return new PersistentDataAccessDTO<K, V>(id, true, key, entities, hashCodeExpected, query, filter, update, collectionName, source, reserved);
	}
	
	public PersistentDataAccessDTO<K, V> shallowCopy(K key, V value) {
		// Presume keyExists=true
		return new PersistentDataAccessDTO<K, V>(id, true, key, value, hashCodeExpected, query, filter, update, collectionName, source, reserved);
	}
	
	public PersistentDataAccessDTO<K, V> shallowCopyWithReserved(Object reserved) {
		return new PersistentDataAccessDTO<K, V>(id, keyExists, key, value, hashCodeExpected, query, filter, update, collectionName, source, reserved);
	}
	
	public PersistentDataAccessDTO<K, V> shallowCopy(UUID source) {
		return new PersistentDataAccessDTO<K, V>(id, keyExists, key, value, hashCodeExpected, query, filter, update, collectionName, source, reserved);
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
