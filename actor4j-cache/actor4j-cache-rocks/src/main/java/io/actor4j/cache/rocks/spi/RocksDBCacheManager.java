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
package io.actor4j.cache.rocks.spi;

import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import io.actor4j.cache.rocks.RocksDBCache;
import io.actor4j.cache.rocks.RocksDBCacheSerializer;

public final class RocksDBCacheManager {
	protected RocksDBCacheManager() {
		super();
	}
	
	public RocksDB createDB(String path) {
		RocksDB result = null;
		
		final Options options = new Options();
		options.setCreateIfMissing(true);
		try {
			result = RocksDB.open(options, path);
		}
		catch (RocksDBException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public <K, V> RocksDBCache<K, V> createCache(String path, RocksDBCacheSerializer<K> keySerializer, RocksDBCacheSerializer<V> valueSerializer) {
		RocksDBCache<K, V> result = null;
		
		RocksDB db = createDB(path);
		if (db!=null)
			result = new RocksDBCache<>(db, keySerializer, valueSerializer);
		
		return result;
	}
}
