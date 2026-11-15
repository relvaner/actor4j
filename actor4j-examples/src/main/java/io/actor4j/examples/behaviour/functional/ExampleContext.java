/*
 * Copyright (c) 2015-2026, David A. Bauer. All rights reserved.
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
package io.actor4j.examples.behaviour.functional;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.actor4j.core.ActorRuntime;
import io.actor4j.core.ActorSystem;
import io.actor4j.core.actors.Actor;
import io.actor4j.core.actors.behaviour.ActorBehaviour;
import io.actor4j.core.actors.behaviour.ActorBehaviourContext;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.messages.ActorMessage;

// behaviours without BehaviourActor: any actor can hold a ActorBehaviourContext
public class ExampleContext {
	public static final int ON = 300;
	public static final int OFF = 301;
	public static final int VALUE = 302;

	// stands for an actor with its own base class
	static class Device extends Actor {
		protected final ActorBehaviour offline = ActorBehaviour.named("OFFLINE", this::offline);
		protected final ActorBehaviour online = ActorBehaviour.named("ONLINE", this::online);
		protected final ActorBehaviourContext behaviours = new ActorBehaviourContext(this, () -> offline);

		protected final CountDownLatch done;

		public Device(String name, CountDownLatch done) {
			super(name);
			this.done = done;
		}

		@Override
		public void receive(ActorMessage<?> message) {
			behaviours.receive(message);
		}

		protected ActorBehaviour offline(ActorMessage<?> message) {
			switch (message.tag()) {
				case ON -> {
					return online;
				}
				case VALUE -> {
					System.out.println("device ["+behaviours.state()+"]: value "+message.value()+" dropped");
					return ActorBehaviour.same();
				}
				default -> {
					return ActorBehaviour.unhandled();
				}
			}
		}

		protected ActorBehaviour online(ActorMessage<?> message) {
			switch (message.tag()) {
				case VALUE -> {
					System.out.println("device ["+behaviours.state()+"]: value "+message.value());
					return ActorBehaviour.same();
				}
				case OFF -> {
					done.countDown();
					return offline;
				}
				default -> {
					return ActorBehaviour.unhandled();
				}
			}
		}
	}

	public static void main(String[] args) throws InterruptedException {
		CountDownLatch done = new CountDownLatch(1);
		ActorSystem system = ActorRuntime.create();
		ActorId device = system.addActor(() -> new Device("device", done));
		system.start();

		system.send(ActorMessage.create(1, VALUE, system.SYSTEM_ID(), device));
		system.send(ActorMessage.create(null, ON, system.SYSTEM_ID(), device));
		system.send(ActorMessage.create(2, VALUE, system.SYSTEM_ID(), device));
		system.send(ActorMessage.create(null, OFF, system.SYSTEM_ID(), device));
		done.await(5, TimeUnit.SECONDS);
		system.shutdownWithActors(true);
	}
}
