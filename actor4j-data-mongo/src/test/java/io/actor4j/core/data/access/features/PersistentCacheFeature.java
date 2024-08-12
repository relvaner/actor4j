/*
 * Copyright (c) 2015-2020, David A. Bauer. All rights reserved.
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
package io.actor4j.core.data.access.features;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import io.actor4j.core.ActorRuntime;
import io.actor4j.core.ActorSystem;
import io.actor4j.core.actors.Actor;
import io.actor4j.core.actors.ActorWithCache;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorGroup;
import io.actor4j.core.utils.ActorGroupSet;
import io.actor4j.core.utils.Pair;
import io.actor4j.core.data.access.PersistentDTO;
import io.actor4j.core.data.access.PrimaryPersistentCacheActor;
import io.actor4j.core.data.access.SecondaryPersistentCacheActor;
import io.actor4j.core.data.access.VolatileDTO;
import io.actor4j.core.data.access.mongo.MongoDataAccessActor;
import io.actor4j.core.data.access.utils.PersistentActorCacheManager;

import static io.actor4j.core.logging.ActorLogger.*;
import static org.junit.Assert.*;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import static io.actor4j.core.data.access.AckMode.*;

public class PersistentCacheFeature {
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
	public void test_primary_secondary_persistent_cache_actor() {
		ActorSystem system = ActorSystem.create(ActorRuntime.factory());
		final int COUNT = 3/*system.getParallelismMin()*system.getParallelismFactor()*/;
		
		CountDownLatch testDone = new CountDownLatch(COUNT);
		
		UUID mediator = system.addActor(() -> new Actor("mediator") {
			protected final String[] keys = {"key4", "key1", "key3", "key2"};
			protected final String[] values = {"value4", "value1", "value3", "value2"};
			protected int i = 0;
			
			@Override 
			public void preStart() {
				UUID dataAccess = system.addActor(() -> new MongoDataAccessActor<String, TestEntity>("dc", client, "actor4j-test", TestEntity.class));
				
				ActorGroup group = new ActorGroupSet();
				AtomicInteger k = new AtomicInteger(0);
				system.addActor(() -> new PrimaryPersistentCacheActor<String, TestEntity>(
						"primary", group, "cache1", (id) -> () -> new SecondaryPersistentCacheActor<String, TestEntity>("secondary-"+k.getAndIncrement(), group, id, 500), COUNT-1, 500, dataAccess, NONE));

				tell(PersistentDTO.create("key1", new TestEntity("key1", "value1"), "key", "test", self()), ActorWithCache.SET, "cache1");
				tell(PersistentDTO.create("key2", new TestEntity("key2", "value2"), "key", "test", self()), ActorWithCache.SET, "cache1");
				tell(PersistentDTO.create("key3", new TestEntity("key3", "value3"), "key", "test", self()), ActorWithCache.SET, "cache1");
				tell(PersistentDTO.create("key4", new TestEntity("key4", "value4"), "key", "test", self()), ActorWithCache.SET, "cache1");
			}
			
			@Override
			public void receive(ActorMessage<?> message) {
				tell(PersistentDTO.create(keys[i], "key", "test", self()), ActorWithCache.GET, "cache1");
				
				await((msg) -> msg.source()!=system.SYSTEM_ID() && msg.value()!=null, (msg) -> {
					@SuppressWarnings("unchecked")
					VolatileDTO<String, TestEntity> payload = ((VolatileDTO<String, TestEntity>)msg.value());
					if (payload.value()!=null) {
						assertEquals(values[i], payload.entity().value);
						logger().log(DEBUG, payload.entity().value);
						if (i<keys.length-1)
							i++;
						testDone.countDown();
					}/*
					else
						logger().debug(false);*/
					unbecome();
				});
			}
		});
		
		system.start();
		
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				system.send(ActorMessage.create(null, 0, system.SYSTEM_ID(), mediator));
			}
		}, 0, 100);
		
		try {
			testDone.await();
			timer.cancel();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		system.shutdownWithActors(true);
	}
	
	@Test(timeout=5000)
	public void test_primary_secondary_persistent_cache_actor_with_manager() {
		ActorSystem system = ActorSystem.create(ActorRuntime.factory());
		final int COUNT = 3/*system.getParallelismMin()*system.getParallelismFactor()*/;
		
		CountDownLatch testDone = new CountDownLatch(COUNT);
		
		UUID mediator = system.addActor(() -> new Actor("mediator") {
			protected PersistentActorCacheManager<String, TestEntity> manager;
			
			protected final String[] keys = {"key4", "key1", "key3", "key2"};
			protected final String[] values = {"value4", "value1", "value3", "value2"};
			protected int i = 0;
			
			@Override 
			public void preStart() {
				UUID dataAccess = system.addActor(() -> new MongoDataAccessActor<String, TestEntity>("dc", client, "actor4j-test", TestEntity.class));
				
				manager = new PersistentActorCacheManager<>(this, "cache1", "key", "test");
				system.addActor(manager.create(COUNT, 500, dataAccess, NONE));
				
				manager.set("key1", new TestEntity("key1", "value1"));
				manager.set("key2", new TestEntity("key2", "value2"));
				manager.set("key3", new TestEntity("key3", "value3"));
				manager.set("key4", new TestEntity("key4", "value4"));
			}
			
			@Override
			public void receive(ActorMessage<?> message) {
				manager.get(keys[i]);
				
				await((msg) -> msg.source()!=system.SYSTEM_ID() && msg.value()!=null, (msg) -> {
					Pair<String, TestEntity> pair = manager.get(msg);
					
					if (pair!=null && pair.b()!=null) {
						assertEquals(keys[i], pair.a());
						assertEquals(keys[i], pair.b().key);
						assertEquals(values[i], pair.b().value);
						logger().log(DEBUG, pair.b().value);
						if (i<keys.length-1)
							i++;
						testDone.countDown();
					}/*
					else
						logger().debug(false);*/
					unbecome();
				});
			}
		});
		
		system.start();
		
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				system.send(ActorMessage.create(null, 0, system.SYSTEM_ID(), mediator));
			}
		}, 0, 100);
		
		try {
			testDone.await();
			timer.cancel();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		system.shutdownWithActors(true);
	}
}
