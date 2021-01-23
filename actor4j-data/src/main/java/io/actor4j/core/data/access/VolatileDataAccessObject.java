/*
 * Copyright (c) 2015-2017, David A. Bauer. All rights reserved.
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

import io.actor4j.core.utils.Shareable;

public class VolatileDataAccessObject<K, V> implements Shareable  {
	public final K key;
	public V value;
	
	public UUID source;
	
	public VolatileDataAccessObject() {
		super();
		this.key = null;
		this.source = null;
	}
	
	public VolatileDataAccessObject(K key) {
		this(key, null, null);
	}
	
	public VolatileDataAccessObject(K key, V value) {
		this(key, value, null);
	}
	
	public VolatileDataAccessObject(K key, UUID source) {
		this(key, null, source);
	}
	
	public VolatileDataAccessObject(K key, V value, UUID source) {
		super();
		this.key = key;
		this.value = value;
		this.source = source;
	}

	@Override
	public String toString() {
		return "VolatileDataAccessObject [key=" + key + ", value=" + value + ", source=" + source + "]";
	}
}
