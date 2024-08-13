/*
 * Copyright (c) 2015-2024, David A. Bauer. All rights reserved.
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
package io.actor4j.cache.rocks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.cache.integration.CacheLoaderException;
import javax.cache.integration.CacheWriterException;

import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.WriteBatch;
import org.rocksdb.WriteOptions;

import io.actor4j.core.utils.Cache;

public class RocksDBCache<K, V> implements Cache<K, V> {
	protected final RocksDB db;
	
	protected final RocksDBCacheSerializer<K> keySerializer;
	protected final RocksDBCacheSerializer<V> valueSerializer;

	public RocksDBCache(RocksDB db, RocksDBCacheSerializer<K> keySerializer, RocksDBCacheSerializer<V> valueSerializer) {
		super();
		
		this.db = db;
		this.keySerializer = keySerializer;
		this.valueSerializer = valueSerializer;
	}
	
	public RocksDB getDB() {
		return db;
	}
	
	@Override
	public boolean containsKey(K key) {
		boolean result = false;
		
		try {
			result = db.keyExists(keySerializer.encode().apply(key));
		} catch(Exception e) {
			throw new CacheLoaderException(e);
		}
		
		return result;
	}

	@Override
	public V get(K key) {
		V result = null;
		
		try {
			byte[] bytes = db.get(keySerializer.encode().apply(key));
			result = valueSerializer.decode().apply(bytes);
		} catch(Exception e) {
			throw new CacheLoaderException(e);
		}
		
		return result;
	}
	
	@Override
	public Map<K, V> get(List<K> keys) {
		Map<K, V> result = new HashMap<>();
		try {
			List<byte[]> values = db.multiGetAsList(keys.stream().map(keySerializer.encode()::apply).collect(Collectors.toList()))
				.stream().filter(value -> value!=null).collect(Collectors.toList());
			if (values!=null) {
				for(int i=0; i<values.size(); i++)
					result.put(keys.get(i), valueSerializer.decode().apply(values.get(i)));
			}
		} catch(RocksDBException e) {
			throw new CacheLoaderException(e);
		}

		return result;
	}

	@Override
	public V put(K key, V value) {
		try {
			db.put(keySerializer.encode().apply(key), valueSerializer.encode().apply(value));
		} catch(Exception e) {
			throw new CacheWriterException(e);
		}
		
		return null;
	}
	
	@Override
	public void put(Map<K, V> entries) {
		try(WriteBatch batch = new WriteBatch()) {
			entries.entrySet()
				.stream()
				.forEach(entry ->
					{
						try {
							batch.put(keySerializer.encode().apply(entry.getKey()), valueSerializer.encode().apply(entry.getValue()));
						} catch (Exception e) {
							throw new CacheWriterException(e);
						}
					}
				);
			db.write(new WriteOptions(), batch);
		} catch(Exception e) {
			throw new CacheWriterException(e);
		}
	}
	
	@Override
	public boolean compareAndSet(K key, V expectedValue, V newValue) {
		// https://github.com/facebook/rocksdb/wiki/Merge-Operator
		
		return false;
	}

	@Override
	public void remove(K key) {
		try {
			db.delete(keySerializer.encode().apply(key));
		} catch(Exception e) {
			throw new CacheWriterException(e);
		}
	}
	
	@Override
	public void remove(List<K> keys) {
		try(WriteBatch batch = new WriteBatch()) {
			keys.stream()
				.forEach(key ->
					{
						try {
							batch.delete(keySerializer.encode().apply(key));
						} catch (Exception e) {
							throw new CacheWriterException(e);
						}
					}
				);
			db.write(new WriteOptions(), batch);
		} catch(Exception e) {
			throw new CacheWriterException(e);
		}
	}

	@Override
	public void clear() {
		try(WriteBatch batch = new WriteBatch(); RocksIterator iterator = db.newIterator()) {
			for (iterator.seekToFirst(); iterator.isValid(); iterator.next())
				batch.delete(iterator.key());
			db.write(new WriteOptions(), batch);
			db.compactRange();
		} catch(Exception e) {
			throw new CacheWriterException(e);
		}
	}

	@Override
	public void evict(long duration) {
		// empty
	}
	
	public void compact() {
		try {
			db.compactRange();
		} catch (RocksDBException e) {
			throw new CacheWriterException(e);
		}
	}
	
	public void close() {
		try {
			db.closeE();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
