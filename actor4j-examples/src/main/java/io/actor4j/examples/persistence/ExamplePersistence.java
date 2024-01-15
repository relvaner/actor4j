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
package io.actor4j.examples.persistence;

import io.actor4j.core.ActorSystem;
import io.actor4j.core.actors.PersistentActor;
import io.actor4j.core.config.ActorSystemConfig;
import io.actor4j.core.json.JsonObject;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.persistence.Recovery;
import io.actor4j.core.persistence.drivers.mongo.MongoDBPersistenceDriver;
import io.actor4j.core.utils.GenericType;
import io.actor4j.examples.shared.ExamplesSettings;

import static io.actor4j.core.logging.ActorLogger.*;

import java.util.UUID;

public class ExamplePersistence {
	static record MyState(String title) {
	}
	
	static record MyEvent(String title) {
	}
	
	public ExamplePersistence() {
		ActorSystemConfig config = ActorSystemConfig.builder()
			.persistenceMode(new MongoDBPersistenceDriver("localhost", 27017, "actor4j"))
			.build();
		ActorSystem system = ActorSystem.create(ExamplesSettings.factory(), config);
		
		UUID id = system.addActor(() -> new PersistentActor<MyState, MyEvent>("example") {
			@Override
			public void receive(ActorMessage<?> message) {
				saveSnapshot(null, null, new MyState("I am a state!"));
				
				MyEvent event1 = new MyEvent("I am the first event!");
				MyEvent event2 = new MyEvent("I am the second event!");
				
				persist(
					(s) -> logger().log(DEBUG, String.format("Event: %s", s)), 
					(e) -> logger().log(ERROR, String.format("Error: %s", e.getMessage())),
					event1, event2);
			}

			@Override
			public void recover(JsonObject value) {
				if (!Recovery.isError(value)) {
					logger().log(DEBUG, String.format("Recovery: %s", value.encodePrettily()));
					Recovery<MyState, MyEvent> obj =  Recovery.convertValue(value, new GenericType<Recovery<MyState, MyEvent>>(){});
					logger().log(DEBUG, String.format("Recovery: %s", obj.toString()));
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
		
		system.start();
		
		system.sendWhenActive(ActorMessage.create(null, 0, system.SYSTEM_ID(), id));
		
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		system.shutdownWithActors(true);
	}
	
	public static void main(String[] args) {
		new ExamplePersistence();
	}
}
