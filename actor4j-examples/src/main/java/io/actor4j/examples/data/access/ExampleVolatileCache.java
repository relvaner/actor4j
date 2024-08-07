/*
 * Copyright (c) 2015-2018, David A. Bauer. All rights reserved.
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
package io.actor4j.examples.data.access;

import java.util.concurrent.CountDownLatch;

import io.actor4j.core.ActorSystem;
import io.actor4j.core.actors.Actor;
import io.actor4j.core.actors.ActorWithCache;
import io.actor4j.core.config.ActorSystemConfig;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.Pair;
import io.actor4j.examples.shared.ExamplesSettings;
import io.actor4j.core.data.access.VolatileFailureDTO;
import io.actor4j.core.data.access.VolatileSuccessDTO;
import io.actor4j.core.data.access.utils.VolatileActorCacheManager;

public class ExampleVolatileCache {
	public ExampleVolatileCache() {
		ActorSystemConfig config = ActorSystemConfig.builder()
			.parallelism(4)
			.build();
		ActorSystem system = ActorSystem.create(ExamplesSettings.factory(), config);
		final int INSTANCES = system.getConfig().parallelism()*system.getConfig().parallelismFactor();
		
		system.addActor(() -> new Actor("manager") {
			protected VolatileActorCacheManager<String, String> manager;
			@Override 
			public void preStart() {
				manager = new VolatileActorCacheManager<String, String>(this, "cache");
				addChild(manager.create(INSTANCES, 500));
				
				manager.set("key1", "value1");
				manager.set("key2", "value2");
				manager.set("key3", "value3");
			}
			
			@Override
			public void receive(ActorMessage<?> message) {
				if (message.tag()==ActorWithCache.SUCCESS && message.value() instanceof VolatileSuccessDTO success)
					System.out.printf("Write success for key: %s%n", success.dto().key().toString());
				else if (message.tag()==ActorWithCache.FAILURE && message.value() instanceof VolatileFailureDTO failure) 
					System.out.printf("Write failure for key: %s%n", failure.dto().key().toString());
			}
		});
		
		system.start();

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		CountDownLatch done = new CountDownLatch(1);
		system.addActor(() -> new Actor("client") {
			protected VolatileActorCacheManager<String, String> manager;
			@Override 
			public void preStart() {
				manager = new VolatileActorCacheManager<String, String>(this, "cache");
				manager.get("key2");
			}
			
			@Override
			public void receive(ActorMessage<?> message) {
				Pair<String, String> pair = manager.get(message);
				if (pair!=null) {
					System.out.printf("value for '%s': %s%n", pair.a(), pair.b());
					done.countDown();
				}
			}
		});
		
		try {
			done.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		system.shutdownWithActors(true);
	}
	
	public static void main(String[] args) {
		new ExampleVolatileCache();
	}
}
