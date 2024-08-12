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

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import io.actor4j.core.ActorSystem;
import io.actor4j.core.actors.Actor;
import io.actor4j.core.actors.ActorWithCache;
import io.actor4j.core.config.ActorSystemConfig;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.Pair;
import io.actor4j.examples.shared.ExamplesSettings;
import io.actor4j.core.data.access.PersistentFailureDTO;
import io.actor4j.core.data.access.PersistentSuccessDTO;
import io.actor4j.core.data.access.mongo.MongoDataAccessActor;
import io.actor4j.core.data.access.utils.PersistentActorCacheManager;

public class ExamplePersistentCache {
	public ExamplePersistentCache() {
		MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
			.applyConnectionString(new ConnectionString("mongodb://localhost:27017"))
			.applyToClusterSettings(builder -> builder.serverSelectionTimeout(5000, TimeUnit.MILLISECONDS))
			.applyToSocketSettings(builder -> {
				builder
					.readTimeout(2000, TimeUnit.MILLISECONDS)
					.connectTimeout(2000, TimeUnit.MILLISECONDS);
	        })
			.build();
		MongoClient mongoClient = MongoClients.create(mongoClientSettings);
		mongoClient.getDatabase("actor4j-test").drop();
		
		ActorSystemConfig config = ActorSystemConfig.builder()
			.parallelism(4)
			.build();
		ActorSystem system = ActorSystem.create(ExamplesSettings.factory(), config);
		final int INSTANCES = system.getConfig().parallelism()*system.getConfig().parallelismFactor();

		system.addActor(() -> new Actor("manager") {
			protected PersistentActorCacheManager<String, ExampleEntity> manager;
			@Override 
			public void preStart() {
				UUID dataAccess = addChild(() -> new MongoDataAccessActor<String, ExampleEntity>("dataAccess", mongoClient, "actor4j-test", ExampleEntity.class));
				
				manager = new PersistentActorCacheManager<String, ExampleEntity>(this, "cache", "key", "cache");
				addChild(manager.create(INSTANCES, 500, dataAccess));
				
				manager.set("key1", new ExampleEntity("key1", "value1"));
				manager.set("key2", new ExampleEntity("key2", "value2"));
				manager.set("key3", new ExampleEntity("key3", "value3"));
			}
			
			@Override
			public void receive(ActorMessage<?> message) {
				if (message.tag()==ActorWithCache.SUCCESS && message.value() instanceof PersistentSuccessDTO success)
					System.out.printf("Write success for key: %s%n", success.dto().key().toString());
				else if (message.tag()==ActorWithCache.FAILURE && message.value() instanceof PersistentFailureDTO failure) 
					System.out.printf("Write failure for key: %s%n", failure.dto().key().toString());
			}
		});
		
		system.start();
		
		try {
			Thread.sleep(5_000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		CountDownLatch done = new CountDownLatch(1);
		system.addActor(() -> new Actor("client") {
			protected PersistentActorCacheManager<String, ExampleEntity> manager;
			@Override 
			public void preStart() {
				manager = new PersistentActorCacheManager<String, ExampleEntity>(this, "cache", "key", "cache");
				manager.get("key2");
			}
			
			@Override
			public void receive(ActorMessage<?> message) {
				Pair<String, ExampleEntity> pair = manager.get(message);
				if (pair!=null) {
					System.out.printf("value for '%s': %s%n", pair.key(), pair.entity().value());
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
		mongoClient.close();
	}
	
	public static void main(String[] args) {
		new ExamplePersistentCache();
	}
}
