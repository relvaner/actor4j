/*
 * Copyright (c) 2015-2023, David A. Bauer. All rights reserved.
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
package io.actor4j.cache.features;

import org.junit.Test;

import io.actor4j.cache.CacheWriterHandler;
import io.actor4j.cache.StorageReader;
import io.actor4j.cache.StorageWriter;
import io.actor4j.cache.runtime.ConcurrentCacheAsMap;
import io.actor4j.cache.runtime.ConcurrentCacheLRU;
import io.actor4j.cache.runtime.ConcurrentCacheVolatileLRU;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ConcurrentCacheFeature {
	@Test
	public void test_cache_lru_with_gc__get_put_resize() {
		ConcurrentCacheVolatileLRU<String, String> cache = new ConcurrentCacheVolatileLRU<>("cacheLRUWithExpiration", 5);
		
		String[][] data = { 
				{"A", "AA"}, 
				{"B", "BB"},
				{"C", "CC"},
				{"D", "DD"},
				{"E", "EE"},
				{"F", "FF"},
				{"G", "GG"}
		};
		
		cache.put(data[0][0], data[0][1]);
		cache.put(data[1][0], data[1][1]);
		cache.put(data[2][0], data[2][1]);
		cache.put(data[3][0], data[3][1]);
		cache.put(data[4][0], data[4][1]);
		
		for (int i=0; i<data.length-2; i++)
			assertEquals(data[i][1], cache.get(data[i][0]));
		
		assertTrue(cache.getMap().size()==5);
		assertTrue(cache.getLru().size()==5);
		cache.put(data[5][0], data[5][1]);
		cache.put(data[6][0], data[6][1]);
		assertTrue(cache.getMap().size()==5);
		assertTrue(cache.getLru().size()==5);
		
		int i=2;
		for (String key : cache.getMap().keySet()) {
			assertEquals(data[i++][0], key);
		}
		
		cache.get(data[5][0]);
		cache.get(data[4][0]);
		
		Iterator<String> iterator = cache.getLru().iterator();
		assertEquals(data[2][0], iterator.next());
		assertEquals(data[3][0], iterator.next());
		assertEquals(data[6][0], iterator.next());
		assertEquals(data[5][0], iterator.next());
		assertEquals(data[4][0], iterator.next());
	}
	
	@Test
	public void test_cache_default__get_put() {
		ConcurrentCacheAsMap<String, String> cache = new ConcurrentCacheAsMap<>("cacheAsMap");
		
		String[][] data = { 
				{"A", "AA"}, 
				{"B", "BB"},
				{"C", "CC"},
				{"D", "DD"},
				{"E", "EE"},
				{"F", "FF"},
				{"G", "GG"}
		};
		
		cache.put(data[0][0], data[0][1]);
		cache.put(data[1][0], data[1][1]);
		cache.put(data[2][0], data[2][1]);
		cache.put(data[3][0], data[3][1]);
		cache.put(data[4][0], data[4][1]);
		
		for (int i=0; i<data.length-2; i++)
			assertEquals(data[i][1], cache.get(data[i][0]));
		
		assertTrue(cache.getMap().size()==5);
		cache.put(data[5][0], data[5][1]);
		cache.put(data[6][0], data[6][1]);
		assertTrue(cache.getMap().size()==7);
		
		int i=0;
		for (String key : cache.getMap().keySet()) {
			assertEquals(data[i++][0], key);
		}
	}
	
	@Test(timeout=5000)
	public void test_cache_lru_async__get_put_resize() {
		Map<String, String> storage = new ConcurrentHashMap<>();
		storage.put("A", "AA");
		storage.put("B", "BB");
		storage.put("C", "CC");
		storage.put("D", "DD");
		storage.put("E", "EE");
		storage.put("F", "FF");
		storage.put("G", "GG");
		
		final Executor readerExecuter = Executors.newFixedThreadPool(4);
		StorageReader<String, String> reader = (key, handler) -> {
			readerExecuter.execute(() -> {
				String value = storage.get(key);
				handler.accept(value);
				// async -> additionally inform client
			});
		};
		
		final Executor writerExecuter = Executors.newSingleThreadExecutor();
		StorageWriter<String, String> writer = new StorageWriter<>() {
			@Override
			public void put(String key, String value, CacheWriterHandler handler) {
				writerExecuter.execute(() -> {
					storage.put(key, value);
					// if no failure, remove dirty bit
					handler.apply();
					// async -> additionally inform client
				});
			}

			@Override
			public void remove(String key, CacheWriterHandler handler) {
				writerExecuter.execute(() -> { 
					storage.remove(key);
					handler.apply();
					// async -> additionally inform client
				});
			}
		};
		
		ConcurrentCacheLRU<String, String> cache = new ConcurrentCacheLRU<>("cacheLRU_Async", 5, reader, writer);
		
		String[][] data = { 
				{"A", "AA"}, 
				{"B", "BB"},
				{"C", "CC"},
				{"D", "DD"},
				{"E", "EE"},
				{"F", "FF"},
				{"G", "GG"}
		};
		
		// Trigger cache misses
		for (int i=0; i<data.length-2; i++)
			assertEquals(null, cache.get(data[i][0]));
		
		while(cache.getMap().size()<5); // Waiting for asyncGet
		
		for (int i=0; i<data.length-2; i++)
			assertEquals(data[i][1], cache.get(data[i][0]));
		
		assertTrue(cache.getMap().size()==5);
		assertTrue(cache.getLru().size()==5);
		cache.put(data[5][0], data[5][1]);
		cache.put(data[6][0], data[6][1]);
		assertTrue(cache.getMap().size()==5);
		assertTrue(cache.getLru().size()==5);

		assertEquals(data[5][1], cache.get(data[5][0])); 
		assertEquals(data[6][1], cache.get(data[6][0]));
		
		int i=2;
		for (String key : cache.getMap().keySet()) {
			assertEquals(data[i++][0], key);
		}
		
		cache.get(data[5][0]);
		cache.get(data[4][0]);

		Iterator<String> iterator = cache.getLru().iterator();
		assertEquals(data[2][0], iterator.next());
		assertEquals(data[3][0], iterator.next());
		assertEquals(data[6][0], iterator.next());
		assertEquals(data[5][0], iterator.next());
		assertEquals(data[4][0], iterator.next());
	}
	
	@Test(timeout=5000)
	public void test_cache_lru__get_put_resize() {
		ConcurrentCacheLRU<String, String> cache = new ConcurrentCacheLRU<>("cacheLRU", 5);
		
		String[][] data = { 
				{"A", "AA"}, 
				{"B", "BB"},
				{"C", "CC"},
				{"D", "DD"},
				{"E", "EE"},
				{"F", "FF"},
				{"G", "GG"}
		};
		
		cache.put(data[0][0], data[0][1]);
		cache.put(data[1][0], data[1][1]);
		cache.put(data[2][0], data[2][1]);
		cache.put(data[3][0], data[3][1]);
		cache.put(data[4][0], data[4][1]);
		
		for (int i=0; i<data.length-2; i++)
			assertEquals(data[i][1], cache.get(data[i][0]));
		
		assertTrue(cache.getMap().size()==5);
		assertTrue(cache.getLru().size()==5);
		cache.put(data[5][0], data[5][1]);
		cache.put(data[6][0], data[6][1]);
		assertTrue(cache.getMap().size()==5);
		assertTrue(cache.getLru().size()==5);
		
		int i=2;
		for (String key : cache.getMap().keySet()) {
			assertEquals(data[i++][0], key);
		}
		
		cache.get(data[5][0]);
		cache.get(data[4][0]);

		Iterator<String> iterator = cache.getLru().iterator();
		assertEquals(data[2][0], iterator.next());
		assertEquals(data[3][0], iterator.next());
		assertEquals(data[6][0], iterator.next());
		assertEquals(data[5][0], iterator.next());
		assertEquals(data[4][0], iterator.next());
	}
}
