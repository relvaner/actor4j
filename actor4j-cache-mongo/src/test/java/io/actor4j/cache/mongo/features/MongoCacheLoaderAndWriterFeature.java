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
package io.actor4j.cache.mongo.features;

import static org.junit.Assert.assertEquals;

import java.awt.Point;

import javax.cache.Cache;
import javax.cache.configuration.MutableConfiguration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import io.actor4j.cache.ActorCacheManager;
import io.actor4j.cache.mongo.MongoCacheConfiguration;
import io.actor4j.cache.mongo.MongoCacheLoaderAndWriter;
import io.actor4j.cache.utils.DummyCacheEntry;

public class MongoCacheLoaderAndWriterFeature {
	protected MongoServer mongoServer;
	protected MongoClient client;
	
	@Before
	public void before() {
		/*
		client.dropDatabase("actor4j-test");
		*/
		
		mongoServer = new MongoServer(new MemoryBackend());
		mongoServer.bind("localhost", 27027);
		
		client = MongoClients.create("mongodb://localhost:27027");
	}
	
	@After
	public void after() {
		client.close();
		mongoServer.shutdown();
	}
	
	@Test(timeout=5000)
	public void test() {
		MongoCacheLoaderAndWriter<String, String> cacheLoaderAndWriter = new MongoCacheLoaderAndWriter<>(
				client, 
				"database",
				"collection01", 
				String.class);
		MutableConfiguration<String, String> configuration = new MutableConfiguration<>();
		configuration
			.setReadThrough(true)
			.setWriteThrough(true)
			.setCacheLoaderFactory(() -> cacheLoaderAndWriter)
			.setCacheWriterFactory(() -> cacheLoaderAndWriter);
		Cache<String, String> cache = ActorCacheManager.createCache("test", configuration);
		
		cache.put("key01", "value01");
		cache.put("key02", "value02");
		cache.put("key03", "value03");
		
		cache.clear();
		
		assertEquals(cache.get("key01"), "value01");
		assertEquals(cache.get("key02"), "value02");
		assertEquals(cache.get("key03"), "value03");
		
		cacheLoaderAndWriter.write(DummyCacheEntry.create("key04", "value04"));
		
		assertEquals(cache.get("key01"), "value01");
		assertEquals(cache.get("key02"), "value02");
		assertEquals(cache.get("key03"), "value03");
		assertEquals(cache.get("key04"), "value04");
	}
	
	@Test(timeout=5000)
	public void test_bulk_1() {
		MongoCacheLoaderAndWriter<String, String> cacheLoaderAndWriter = new MongoCacheLoaderAndWriter<>(
				client, 
				"database",
				"collection02", 
				String.class,
				1);
		MutableConfiguration<String, String> configuration = new MutableConfiguration<>();
		configuration
			.setReadThrough(true)
			.setWriteThrough(true)
			.setCacheLoaderFactory(() -> cacheLoaderAndWriter)
			.setCacheWriterFactory(() -> cacheLoaderAndWriter);
		Cache<String, String> cache = ActorCacheManager.createCache("test_bulk_1", configuration);
		
		cache.put("key01", "value01");
		cache.put("key02", "value02");
		cache.put("key03", "value03");
		
		cache.clear();
		
		assertEquals(cache.get("key01"), "value01");
		assertEquals(cache.get("key02"), "value02");
		assertEquals(cache.get("key03"), "value03");
		
		cacheLoaderAndWriter.write(DummyCacheEntry.create("key04", "value04"));
		
		assertEquals(cache.get("key01"), "value01");
		assertEquals(cache.get("key02"), "value02");
		assertEquals(cache.get("key03"), "value03");
		assertEquals(cache.get("key04"), "value04");
	}
	
	@Test(timeout=5000)
	public void test_bulk_2() {
		MongoCacheLoaderAndWriter<String, String> cacheLoaderAndWriter = new MongoCacheLoaderAndWriter<>(
				client, 
				"database",
				"collection03", 
				String.class,
				2);
		MutableConfiguration<String, String> configuration = new MutableConfiguration<>();
		configuration
			.setReadThrough(true)
			.setWriteThrough(true)
			.setCacheLoaderFactory(() -> cacheLoaderAndWriter)
			.setCacheWriterFactory(() -> cacheLoaderAndWriter);
		Cache<String, String> cache = ActorCacheManager.createCache("test_bulk_2", configuration);
		
		cache.put("key01", "value01");
		cache.put("key02", "value02");
		cache.put("key03", "value03");
		
		cache.clear();
		
		assertEquals(cache.get("key01"), "value01");
		assertEquals(cache.get("key02"), "value02");
		assertEquals(cache.get("key03"), null);
		
		cacheLoaderAndWriter.flush();
		
		cacheLoaderAndWriter.write(DummyCacheEntry.create("key04", "value04"));

		assertEquals(cache.get("key01"), "value01");
		assertEquals(cache.get("key02"), "value02");
		assertEquals(cache.get("key03"), "value03");
		assertEquals(cache.get("key04"), null);
		
		cacheLoaderAndWriter.flush();
		
		assertEquals(cache.get("key01"), "value01");
		assertEquals(cache.get("key02"), "value02");
		assertEquals(cache.get("key03"), "value03");
		assertEquals(cache.get("key04"), "value04");
	}
	
	@Test(timeout=5000)
	public void test_bulk_10() {
		MongoCacheLoaderAndWriter<String, String> cacheLoaderAndWriter = new MongoCacheLoaderAndWriter<>(
				client, 
				"database",
				"collection04", 
				String.class,
				10);
		MutableConfiguration<String, String> configuration = new MutableConfiguration<>();
		configuration
			.setReadThrough(true)
			.setWriteThrough(true)
			.setCacheLoaderFactory(() -> cacheLoaderAndWriter)
			.setCacheWriterFactory(() -> cacheLoaderAndWriter);
		Cache<String, String> cache = ActorCacheManager.createCache("test_bulk_10", configuration);
		
		cache.put("key01", "value01");
		cache.put("key02", "value02");
		cache.put("key03", "value03");
		
		cache.clear();
		
		assertEquals(cache.get("key01"), null);
		assertEquals(cache.get("key02"), null);
		assertEquals(cache.get("key03"), null);
		
		cacheLoaderAndWriter.write(DummyCacheEntry.create("key04", "value04"));

		assertEquals(cache.get("key01"), null);
		assertEquals(cache.get("key02"), null);
		assertEquals(cache.get("key03"), null);
		assertEquals(cache.get("key04"), null);
		
		cacheLoaderAndWriter.flush();
		
		assertEquals(cache.get("key01"), "value01");
		assertEquals(cache.get("key02"), "value02");
		assertEquals(cache.get("key03"), "value03");
		assertEquals(cache.get("key04"), "value04");
	}
	
	@Test(timeout=5000)
	public void test_point() {
		MongoCacheConfiguration<Integer, Point> configuration = new MongoCacheConfiguration<>(
			client, 
			"database",
			"collection05", 
			Point.class);
		Cache<Integer, Point> cache = ActorCacheManager.createCache("test_point", configuration);
		
		cache.put(100, new Point(12, 55));
		cache.put(101, new Point(13, 56));
		cache.put(102, new Point(14, 57));
		
		cache.clear();
		
		assertEquals(cache.get(100).x, 12);
		assertEquals(cache.get(101).y, 56);
		assertEquals(cache.get(102).x, 14);
		
		configuration.getCacheLoaderAndWriter().write(DummyCacheEntry.create(103, new Point(15, 58)));
		
		assertEquals(cache.get(100).x, 12);
		assertEquals(cache.get(101).y, 56);
		assertEquals(cache.get(102).x, 14);
		assertEquals(cache.get(103).y, 58);
	}
}
