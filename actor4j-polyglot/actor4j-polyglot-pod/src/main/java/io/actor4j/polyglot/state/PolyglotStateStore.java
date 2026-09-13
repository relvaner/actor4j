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
package io.actor4j.polyglot.state;

import java.util.List;
import java.util.Map;

import org.graalvm.polyglot.HostAccess.Export;

import io.actor4j.core.utils.Cache;

public class PolyglotStateStore implements Cache<Object, Object>{
	protected final Cache<Object, Object> delegate;
	
	public PolyglotStateStore(Cache<Object, Object> delegate) {
		this.delegate = delegate;
	}
	
	public static PolyglotStateStore create(Cache<Object, Object> delegate) {
		return new PolyglotStateStore(delegate);
	}

	@Export
	@Override
	public boolean containsKey(Object key) {
		return delegate.containsKey(key);
	}

	@Export
	@Override
	public Object get(Object key) {
		return delegate.get(key);
	}

	@Export
	@Override
	public Map<Object, Object> get(List<Object> keys) {
		return delegate.get(keys);
	}

	@Export
	@Override
	public Object put(Object key, Object value) {
		return delegate.put(key, value);
	}

	@Export
	@Override
	public void put(Map<Object, Object> entries) {
		delegate.put(entries);
	}

	@Export
	@Override
	public boolean compareAndSet(Object key, Object expectedValue, Object newValue) {
		return delegate.compareAndSet(key, expectedValue, newValue);
	}

	@Export
	@Override
	public void remove(Object key) {
		delegate.remove(key);
	}

	@Export
	@Override
	public void remove(List<Object> keys) {
		delegate.remove(keys);
	}

	@Export
	@Override
	public void clear() {
		delegate.clear();
	}

	@Export
	@Override
	public void evict(long duration) {
		delegate.evict(duration);
	}

	@Export
	@Override
	public void close() {
		delegate.close();
	}
}
