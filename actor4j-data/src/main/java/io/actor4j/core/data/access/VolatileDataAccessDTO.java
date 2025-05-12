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

import java.util.List;
import java.util.UUID;

import io.actor4j.core.id.ActorId;

public record VolatileDataAccessDTO<K, V>(UUID id, boolean keyExists, K key, Object value, int hashCodeExpected, ActorId source, Object reserved) implements VolatileDTO<K, V>  {
	public VolatileDataAccessDTO(K key, V value, int hashCodeExpected, ActorId source) {
		this(UUID.randomUUID(), false, key, value, hashCodeExpected, source, null);
	}
	
	public VolatileDataAccessDTO(K key, V value, ActorId source) {
		this(UUID.randomUUID(), false, key, value, 0, source, null);
	}
	
	public VolatileDataAccessDTO(ActorId source) {
		this(null, null, source);
	}
	
	public VolatileDataAccessDTO(K key, ActorId source) {
		this(key, null, source);
	}
	
	public VolatileDataAccessDTO<K, V> shallowCopy(V value) {
		// Presume keyExists=true
		return new VolatileDataAccessDTO<K, V>(id, true, key, value, hashCodeExpected, source, reserved);
	}
	
	public VolatileDataAccessDTO<K, V> shallowCopyWithReserved(Object reserved) {
		return new VolatileDataAccessDTO<K, V>(id, keyExists, key, value, hashCodeExpected, source, reserved);
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
