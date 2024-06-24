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
package io.actor4j.jcache.mongo.features;

import static org.junit.Assert.assertEquals;

import java.awt.Point;

import javax.cache.Cache;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import io.actor4j.jcache.mongo.MongoCacheConfiguration;
import io.actor4j.jcache.spi.CachingProvider;
import io.actor4j.jcache.spi.LocalActorCacheManager;
import io.actor4j.jcache.utils.DummyCacheEntry;

public class MongoCacheLoaderAndWriterFeature {
	protected MongoServer mongoServer;
	protected MongoClient client;
	protected LocalActorCacheManager cacheManager;
	
	@Before
	public void before() {
		/*
		client.dropDatabase("actor4j-test");
		*/
		
		mongoServer = new MongoServer(new MemoryBackend());
		mongoServer.bind("localhost", 27027);
		
		client = MongoClients.create("mongodb://localhost:27027");
		
		cacheManager = CachingProvider.getCachingProvider().getCacheManager();
	}
	
	@After
	public void after() {
		client.close();
		mongoServer.shutdown();
	}
	
	@Test(timeout=5000)
	public void test() {
		MongoCacheConfiguration<String, String> configuration = new MongoCacheConfiguration<>();
		configuration
			.setMongoClient(client)
			.setDatabaseName("database")
			.setCollectionName("collection01")
			.setValueType(String.class)
			.build();
		Cache<String, String> cache = cacheManager.createCache("test", configuration);
		
		cache.put("key01", "value01");
		cache.put("key02", "value02");
		cache.put("key03", "value03");
		
		cache.clear();
		
		assertEquals(cache.get("key01"), "value01");
		assertEquals(cache.get("key02"), "value02");
		assertEquals(cache.get("key03"), "value03");
		
		configuration.getCacheLoaderAndWriter().write(DummyCacheEntry.create("key04", "value04"));
		
		assertEquals(cache.get("key01"), "value01");
		assertEquals(cache.get("key02"), "value02");
		assertEquals(cache.get("key03"), "value03");
		assertEquals(cache.get("key04"), "value04");
	}
	
	@Test(timeout=5000)
	public void test_bulk_1() {
		MongoCacheConfiguration<String, String> configuration = new MongoCacheConfiguration<>();
		configuration
			.setMongoClient(client)
			.setDatabaseName("database")
			.setCollectionName("collection02")
			.setValueType(String.class)
			.setBulkSize(1)
			.build();
		Cache<String, String> cache = cacheManager.createCache("test_bulk_1", configuration);
		
		cache.put("key01", "value01");
		cache.put("key02", "value02");
		cache.put("key03", "value03");
		
		cache.clear();
		
		assertEquals(cache.get("key01"), "value01");
		assertEquals(cache.get("key02"), "value02");
		assertEquals(cache.get("key03"), "value03");
		
		configuration.getCacheLoaderAndWriter().write(DummyCacheEntry.create("key04", "value04"));
		
		assertEquals(cache.get("key01"), "value01");
		assertEquals(cache.get("key02"), "value02");
		assertEquals(cache.get("key03"), "value03");
		assertEquals(cache.get("key04"), "value04");
	}
	
	@Test(timeout=5000)
	public void test_bulk_2() {
		MongoCacheConfiguration<String, String> configuration = new MongoCacheConfiguration<>();
		configuration
			.setMongoClient(client)
			.setDatabaseName("database")
			.setCollectionName("collection03")
			.setValueType(String.class)
			.setBulkSize(2)
			.build();
		Cache<String, String> cache = cacheManager.createCache("test_bulk_2", configuration);
		
		cache.put("key01", "value01");
		cache.put("key02", "value02");
		cache.put("key03", "value03");
		
		cache.clear();
		
		assertEquals(cache.get("key01"), "value01");
		assertEquals(cache.get("key02"), "value02");
		assertEquals(cache.get("key03"), null);
		
		configuration.getCacheLoaderAndWriter().flush();
		
		configuration.getCacheLoaderAndWriter().write(DummyCacheEntry.create("key04", "value04"));

		assertEquals(cache.get("key01"), "value01");
		assertEquals(cache.get("key02"), "value02");
		assertEquals(cache.get("key03"), "value03");
		assertEquals(cache.get("key04"), null);
		
		configuration.getCacheLoaderAndWriter().flush();
		
		assertEquals(cache.get("key01"), "value01");
		assertEquals(cache.get("key02"), "value02");
		assertEquals(cache.get("key03"), "value03");
		assertEquals(cache.get("key04"), "value04");
	}
	
	@Test(timeout=5000)
	public void test_bulk_10() {
		MongoCacheConfiguration<String, String> configuration = new MongoCacheConfiguration<>();
		configuration
			.setMongoClient(client)
			.setDatabaseName("database")
			.setCollectionName("collection04")
			.setValueType(String.class)
			.setBulkSize(10)
			.build();
		Cache<String, String> cache = cacheManager.createCache("test_bulk_10", configuration);
		
		cache.put("key01", "value01");
		cache.put("key02", "value02");
		cache.put("key03", "value03");
		
		cache.clear();
		
		assertEquals(cache.get("key01"), null);
		assertEquals(cache.get("key02"), null);
		assertEquals(cache.get("key03"), null);
		
		configuration.getCacheLoaderAndWriter().write(DummyCacheEntry.create("key04", "value04"));

		assertEquals(cache.get("key01"), null);
		assertEquals(cache.get("key02"), null);
		assertEquals(cache.get("key03"), null);
		assertEquals(cache.get("key04"), null);
		
		configuration.getCacheLoaderAndWriter().flush();
		
		assertEquals(cache.get("key01"), "value01");
		assertEquals(cache.get("key02"), "value02");
		assertEquals(cache.get("key03"), "value03");
		assertEquals(cache.get("key04"), "value04");
	}
	
	@Test(timeout=5000)
	public void test_point() {
		MongoCacheConfiguration<Integer, Point> configuration = new MongoCacheConfiguration<>();
		configuration
			.setMongoClient(client)
			.setDatabaseName("database")
			.setCollectionName("collection05")
			.setValueType(Point.class)
			.build();
		Cache<Integer, Point> cache = cacheManager.createCache("test_point", configuration);
		
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
