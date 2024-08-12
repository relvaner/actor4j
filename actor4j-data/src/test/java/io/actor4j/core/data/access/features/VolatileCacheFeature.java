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
package io.actor4j.core.data.access.features;

import org.junit.Test;

import io.actor4j.core.ActorSystem;
import io.actor4j.core.actors.Actor;
import io.actor4j.core.actors.ActorWithCache;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorGroup;
import io.actor4j.core.utils.ActorGroupSet;
import io.actor4j.core.data.access.PrimaryVolatileCacheActor;
import io.actor4j.core.data.access.SecondaryVolatileCacheActor;
import io.actor4j.core.data.access.VolatileDTO;
import io.actor4j.core.data.access.utils.VolatileActorCacheManager;

import static io.actor4j.core.logging.ActorLogger.*;
import static org.junit.Assert.*;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import static io.actor4j.core.data.access.AckMode.*;

public class VolatileCacheFeature {

	@Test(timeout=5000)
	public void test_primary_secondary_volatile_cache_actor() {
		ActorSystem system = ActorSystem.create(AllFeaturesTest.factory());
		final int COUNT = 3/*system.getParallelismMin()*system.getParallelismFactor()*/;
		
		CountDownLatch testDone = new CountDownLatch(12);
		
		UUID mediator = system.addActor(() -> new Actor("mediator") {
			protected final String[] keys = {"key1", "key2", "key3", "key4"};
			protected final String[] values = {"value1", "value2", "value3", "value4"};
			protected int i = 0;
			
			@Override 
			public void preStart() {
				ActorGroup group = new ActorGroupSet();
				AtomicInteger k = new AtomicInteger(0);
				system.addActor(() -> new PrimaryVolatileCacheActor<String, String>(
						"primary", group, "vcache1", (id) -> () -> new SecondaryVolatileCacheActor<String, String>("secondary-"+k.getAndIncrement(), group, id, 500), COUNT-1, 500, NONE));

				tell(VolatileDTO.create("key1", "value1", self()), ActorWithCache.SET, "vcache1");
				tell(VolatileDTO.create("key2", "value2", self()), ActorWithCache.SET, "vcache1");
				tell(VolatileDTO.create("key3", "value3", self()), ActorWithCache.SET, "vcache1");
				tell(VolatileDTO.create("key4", "value4", self()), ActorWithCache.SET, "vcache1");
			}
			
			@Override
			public void receive(ActorMessage<?> message) {
				if (i<4) {
					tell(VolatileDTO.create(keys[i], self()), ActorWithCache.GET, "vcache1");
					
					await((msg) -> msg.source()!=system.SYSTEM_ID() && msg.value()!=null && msg.tag()==ActorWithCache.GET, (msg) -> {
						@SuppressWarnings("unchecked")
						VolatileDTO<String, String> payload = ((VolatileDTO<String, String>)msg.value());
						if (payload.value()!=null) {
							assertEquals(values[i], payload.value());
							logger().log(DEBUG, payload.value().toString());
							i++;
							testDone.countDown();
						}/*
						else
							logger().debug(false);*/
						unbecome();
					});
				}
				else if (i==4) {
					tell(VolatileDTO.create(keys[2], self()), ActorWithCache.DEL, "vcache1");
					become((msg) -> {
						if (i<5) {
							tell(VolatileDTO.create(keys[2], self()), ActorWithCache.GET, "vcache1");
							await((msg2) -> msg2.source()!=system.SYSTEM_ID() && msg2.value()!=null && msg2.tag()==ActorWithCache.GET, (msg2) -> {
								@SuppressWarnings("unchecked")
								VolatileDTO<String, String> payload = ((VolatileDTO<String, String>)msg2.value());
								if (payload.value()==null) {
									assertNull(payload.value());
									logger().log(DEBUG, "null");
									i++;
									testDone.countDown();
								}
								unbecome();
							});
						}
						else {
							unbecome();
						}
					});
				}
				else if (i>4 && i<8) {
					if (i==7)
						i++;
					tell(VolatileDTO.create(keys[i-5], self()), ActorWithCache.GET, "vcache1");
					await((msg) -> msg.source()!=system.SYSTEM_ID() && msg.value()!=null && msg.tag()==ActorWithCache.GET, (msg) -> {
						@SuppressWarnings("unchecked")
						VolatileDTO<String, String> payload = ((VolatileDTO<String, String>)msg.value());
						if (payload.value()!=null) {
							assertEquals(values[i-5], payload.value());
							logger().log(DEBUG, payload.value().toString());
							i++;
							testDone.countDown();
						}
						unbecome();
					});
				}
				else if (i>8) {
					tell(VolatileDTO.create(self()), ActorWithCache.DEL_ALL, "vcache1");
					become((msg) -> {
						if (i<13) {
							tell(VolatileDTO.create(keys[i-9], self()), ActorWithCache.GET, "vcache1");
							await((msg2) -> msg2.source()!=system.SYSTEM_ID() && msg2.value()!=null && msg2.tag()==ActorWithCache.GET, (msg2) -> {
								@SuppressWarnings("unchecked")
								VolatileDTO<String, String> payload = ((VolatileDTO<String, String>)msg2.value());
								if (keys[i-9].equals(payload.key())) {
									assertNull(payload.value());
									logger().log(DEBUG, "null");
									i++;
									testDone.countDown();
								}
								unbecome();
							});
						}
					});
				}
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
	public void test_primary_secondary_volatile_cache_actor_with_manager() {
		ActorSystem system = ActorSystem.create(AllFeaturesTest.factory());
		final int COUNT = 3/*system.getParallelismMin()*system.getParallelismFactor()*/;
		
		CountDownLatch testDone = new CountDownLatch(12);
		
		UUID mediator = system.addActor(() -> new Actor("mediator") {
			protected VolatileActorCacheManager<String, String> manager;
			
			protected final String[] keys = {"key1", "key2", "key3", "key4"};
			protected final String[] values = {"value1", "value2", "value3", "value4"};
			protected int i = 0;
			
			@Override 
			public void preStart() {
				manager = new VolatileActorCacheManager<String, String>(this, "vcache1");
				system.addActor(manager.create(COUNT, 500, NONE));

				manager.set("key1", "value1");
				manager.set("key2", "value2");
				manager.set("key3", "value3");
				manager.set("key4", "value4");
			}
			
			@Override
			public void receive(ActorMessage<?> message) {
				if (i<4) {
					manager.get(keys[i]);
					
					await((msg) -> msg.source()!=system.SYSTEM_ID() && msg.value()!=null && msg.tag()==ActorWithCache.GET, (msg) -> {
						@SuppressWarnings("unchecked")
						VolatileDTO<String, String> payload = ((VolatileDTO<String, String>)msg.value());
						if (payload.value()!=null) {
							assertEquals(values[i], payload.value());
							logger().log(DEBUG, payload.value().toString());
							i++;
							testDone.countDown();
						}/*
						else
							logger().debug(false);*/
						unbecome();
					});
				}
				else if (i==4) {
					manager.del(keys[2]);
					become((msg) -> {
						if (i<5) {
							manager.get(keys[2]);
							await((msg2) -> msg2.source()!=system.SYSTEM_ID() && msg2.value()!=null && msg2.tag()==ActorWithCache.GET, (msg2) -> {
								@SuppressWarnings("unchecked")
								VolatileDTO<String, String> payload = ((VolatileDTO<String, String>)msg2.value());
								if (payload.value()==null) {
									assertNull(payload.value());
									logger().log(DEBUG, "null");
									i++;
									testDone.countDown();
								}
								unbecome();
							});
						}
						else {
							unbecome();
						}
					});
				}
				else if (i>4 && i<8) {
					if (i==7)
						i++;
					manager.get(keys[i-5]);
					await((msg) -> msg.source()!=system.SYSTEM_ID() && msg.value()!=null && msg.tag()==ActorWithCache.GET, (msg) -> {
						@SuppressWarnings("unchecked")
						VolatileDTO<String, String> payload = ((VolatileDTO<String, String>)msg.value());
						if (payload.value()!=null) {
							assertEquals(values[i-5], payload.value());
							logger().log(DEBUG, payload.value().toString());
							i++;
							testDone.countDown();
						}
						unbecome();
					});
				}
				else if (i>8) {
					manager.delAll();
					become((msg) -> {
						if (i<13) {
							manager.get(keys[i-9]);
							await((msg2) -> msg2.source()!=system.SYSTEM_ID() && msg2.value()!=null && msg2.tag()==ActorWithCache.GET, (msg2) -> {
								@SuppressWarnings("unchecked")
								VolatileDTO<String, String> payload = ((VolatileDTO<String, String>)msg2.value());
								if (keys[i-9].equals(payload.key())) {
									assertNull(payload.value());
									logger().log(DEBUG, "null");
									i++;
									testDone.countDown();
								}
								unbecome();
							});
						}
					});
				}
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
