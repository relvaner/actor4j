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

import java.util.UUID;

import io.actor4j.core.json.JsonObject;

public record PersistentDataAccessDTO<K, V>(K key, V value, int hashCodeExpected, JsonObject filter, JsonObject update, String collectionName, UUID source, Object reserved) implements PersistentDTO<K, V> {
	public PersistentDataAccessDTO(K key, V value, String keyname, String collectionName) {
		this(key, value, 0, JsonObject.create().put(keyname, key), null, collectionName, null, null);
	}
	
	public PersistentDataAccessDTO(K key, String keyname, String collectionName, UUID source) {
		this(key, null, 0, JsonObject.create().put(keyname, key), null, collectionName, source, null);
	}
	
	public PersistentDataAccessDTO(K key, V value, String keyname, JsonObject update, String collectionName) {
		this(key, value, 0, keyname!=null ? JsonObject.create().put(keyname, key) : null, update, collectionName, null, null);
	}
	
	public PersistentDataAccessDTO(K key, V value, JsonObject filter, JsonObject update, String collectionName, UUID source) {
		this(key, value, 0, filter, update, collectionName, source, null);
	}
	
	public PersistentDataAccessDTO(K key, V value, String collectionName) {
		this(key, value, 0, null, null, collectionName, null, null);
	}
	
	public PersistentDataAccessDTO(K key, String collectionName, UUID source) {
		this(key, null, 0, null, null, collectionName, source, null);
	}
	
	public PersistentDataAccessDTO<K, V> shallowCopy(V value) {
		return new PersistentDataAccessDTO<K, V>(key, value, hashCodeExpected, filter, update, collectionName, source, reserved);
	}
	
	public PersistentDataAccessDTO<K, V> shallowCopyWithReserved(Object reserved) {
		return new PersistentDataAccessDTO<K, V>(key, value, hashCodeExpected, filter, update, collectionName, source, reserved);
	}
}
