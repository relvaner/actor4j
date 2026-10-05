/*
 * Copyright (c) 2015-2026, David A. Bauer. All rights reserved.
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
package io.actor4j.functions.state;

import java.util.List;
import java.util.Map;

import io.actor4j.core.utils.Cache;

public class StateStore<K, V> implements Cache<K, V>{
	protected final Cache<K, V> delegate;
	
	public StateStore(Cache<K, V> delegate) {
		this.delegate = delegate;
	}
	
	public static <K, V> StateStore<K, V> create(Cache<K, V> delegate) {
		return new StateStore<K, V>(delegate);
	}

	@Override
	public boolean containsKey(K key) {
		return delegate.containsKey(key);
	}

	@Override
	public V get(K key) {
		return delegate.get(key);
	}

	@Override
	public Map<K, V> get(List<K> keys) {
		return delegate.get(keys);
	}

	@Override
	public V put(K key, V value) {
		return delegate.put(key, value);
	}

	@Override
	public void put(Map<K, V> entries) {
		delegate.put(entries);
	}

	@Override
	public boolean compareAndSet(K key, V expectedValue, V newValue) {
		return delegate.compareAndSet(key, expectedValue, newValue);
	}

	@Override
	public void remove(K key) {
		delegate.remove(key);
	}

	@Override
	public void remove(List<K> keys) {
		delegate.remove(keys);
	}

	@Override
	public void clear() {
		delegate.clear();
	}

	@Override
	public void evict(long duration) {
		delegate.evict(duration);
	}

	@Override
	public void close() {
		delegate.close();
	}
}
