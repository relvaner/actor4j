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
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.persistence.ActorPersistenceObject;
import io.actor4j.core.persistence.Recovery;
import io.actor4j.core.persistence.drivers.mongo.MongoDBPersistenceDriver;

import static io.actor4j.core.logging.ActorLogger.*;

import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;

public class ExamplePersistence {
	static class MyState extends ActorPersistenceObject {
		public String title;
		
		public MyState() {
			super();
		}

		public MyState(String title) {
			super();
			this.title = title;
		}

		@Override
		public String toString() {
			return "MyState [title=" + title + ", persistenceId=" + persistenceId + ", timeStamp=" + timeStamp
					+ ", index=" + index + "]";
		}
	}
	
	static class MyEvent extends ActorPersistenceObject {
		public String title;
		
		public MyEvent() {
			super();
		}

		public MyEvent(String title) {
			super();
			this.title = title;
		}

		@Override
		public String toString() {
			return "MyEvent [title=" + title + ", persistenceId=" + persistenceId + ", timeStamp=" + timeStamp
					+ ", index=" + index + "]";
		}
	}
	
	public ExamplePersistence() {
		ActorSystemConfig config = ActorSystemConfig.builder()
			.persistenceMode(new MongoDBPersistenceDriver("localhost", 27017, "actor4j"))
			.build();
		ActorSystem system = ActorSystem.create(config);
		
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
			public void recover(String json) {
				if (!Recovery.isError(json)) {
					logger().log(DEBUG, String.format("Recovery: %s", json));
					Recovery<MyState, MyEvent> obj = Recovery.convertValue(json, new TypeReference<Recovery<MyState, MyEvent>>(){});
					logger().log(DEBUG, String.format("Recovery: %s", obj.toString()));
				}
				else
					logger().log(ERROR, String.format("Error: %s", Recovery.getErrorMsg(json)));
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
