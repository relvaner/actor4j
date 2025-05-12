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
package io.actor4j.core.persistence.features;

import io.actor4j.core.ActorRuntime;
import io.actor4j.core.ActorSystem;
import io.actor4j.core.actors.Actor;
import io.actor4j.core.actors.PersistentActor;
import io.actor4j.core.config.ActorSystemConfig;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.json.JsonObject;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.persistence.Recovery;
import io.actor4j.core.persistence.drivers.mongo.MongoDBPersistenceDriver;
import io.actor4j.core.utils.GenericType;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;

import static io.actor4j.core.logging.ActorLogger.*;
import static org.junit.Assert.*;

public class PersistenceFeature {
	static record MyState(String title) {
	}
	
	static record MyEvent(String title) {
	}
	
	//@Ignore("Works only until MongoDB Java Driver 3.6.4, with Fongo 2.2.0-RC2")
	@Test(timeout=30000)
	public void test() {
		CountDownLatch testDone = new CountDownLatch(2);
		
		ActorSystemConfig config = ActorSystemConfig.builder()
			.persistenceMode(new MongoDBPersistenceDriver("localhost", 27027, "actor4j-test"))
			.build();
		ActorSystem system = ActorSystem.create(ActorRuntime.factory(), config);
		
		AtomicBoolean first = new AtomicBoolean(true);
		ActorId id = system.addActor(() -> new PersistentActor<MyState, MyEvent>("example") {
			@Override
			public void receive(ActorMessage<?> message) {
				saveSnapshot(null, null, new MyState("I am the first state!"));
				
				MyEvent event1 = new MyEvent("I am the first event!");
				
				persist(
					(s) -> logger().log(DEBUG, String.format("Event: %s", s)), 
					(e) -> logger().log(ERROR, String.format("Error: %s", e.getMessage())),
					event1);
				
				saveSnapshot(null, null, new MyState("I am the second state!"));
				
				MyEvent event2 = new MyEvent("I am the second event!");
				MyEvent event3 = new MyEvent("I am the third event!");
				MyEvent event4 = new MyEvent("I am the fourth event!");
				
				persist(
						(s) -> logger().log(DEBUG, String.format("Event: %s", s)), 
						(e) -> logger().log(ERROR, String.format("Error: %s", e.getMessage())),
						event2, event3, event4);
				
				if (first.getAndSet(false))
					tell(null, Actor.RESTART, self());
			}

			@Override
			public void recover(JsonObject value) {
				if (!Recovery.isError(value)) {
					logger().log(DEBUG, String.format("Recovery: %s", value.encodePrettily()));
					Recovery<MyState, MyEvent> obj = Recovery.convertValue(value, new GenericType<Recovery<MyState, MyEvent>>(){});
					logger().log(DEBUG, String.format("Recovery: %s", obj.toString()));
					if (first.get())
						assertEquals("{\"state\":{}}", value.encode());
					else {
						assertEquals("I am the second state!", obj.state().value().title());
						assertTrue(obj.events().size()==3);
						assertEquals("I am the second event!", obj.events().get(0).value().title());
						assertEquals("I am the third event!", obj.events().get(1).value().title());
						assertEquals("I am the fourth event!", obj.events().get(2).value().title());
					}
					testDone.countDown();
				}
				else
					logger().log(ERROR, String.format("Error: %s", Recovery.getErrorMsg(value)));
			}
			
			@Override
			public UUID persistenceId() {
				/* e.g. https://www.uuidgenerator.net/ */
				return UUID.fromString("60f086af-27d3-44e9-8fd7-eb095c98daed");
			}
		});
		
		// Drop database
		/*
		MongoClient client = new MongoClient("localhost", 27017);
		client.dropDatabase("actor4j-test");
		client.close();
		*/
		MongoServer mongoServer = new MongoServer(new MemoryBackend());
		mongoServer.bind("localhost", 27027);
		
		system.start();
		
		system.sendWhenActive(ActorMessage.create(null, 0, system.SYSTEM_ID(), id));
		
		try {
			testDone.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		system.shutdownWithActors(true);
		mongoServer.shutdown();
	}
}
