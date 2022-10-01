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

public interface PersistentDTO<K, V> extends VolatileDTO<K, V> {
	public int hashCodeExpected();
	public String filter();
	public String update();
	public String collectionName();
	
	public static <K, V> PersistentDataAccessObject<K, V> create(K key, V value, String keyname, String collectionName) {
		return new PersistentDataAccessObject<K, V>(key, value, keyname, collectionName);
	}
	
	public static <K, V> PersistentDataAccessObject<K, V> create(K key, String keyname, String collectionName, UUID source) {
		return new PersistentDataAccessObject<K, V>(key, keyname, collectionName, source);
	}
	
	public static <K, V> PersistentDataAccessObject<K, V> create(K key, V value, String keyname, String update, String collectionName) {
		return new PersistentDataAccessObject<K, V>(key, value, keyname, update, collectionName);
	}
	
	public static <K, V> PersistentDataAccessObject<K, V> create(K key, V value, String filter, String update, String collectionName, UUID source) {
		return new PersistentDataAccessObject<K, V>(key, value, filter, update, collectionName, source);
	}
	
	public static <K, V> PersistentDataAccessObject<K, V> create(K key, V value, String collectionName) {
		return new PersistentDataAccessObject<K, V>(key, value, collectionName);
	}
	
	public static <K, V> PersistentDataAccessObject<K, V> create(K key, String collectionName, UUID source) {
		return new PersistentDataAccessObject<K, V>(key, collectionName, source);
	}
}
