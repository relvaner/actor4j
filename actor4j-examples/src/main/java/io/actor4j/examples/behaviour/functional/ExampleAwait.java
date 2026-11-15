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
import io.actor4j.core.actors.behaviour.ActorBehaviour;
import io.actor4j.core.actors.behaviour.BehaviourActor;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.messages.ActorMessage;

// await as behaviour: waits for the response, other messages are handled as usual (not swallowed)
public class ExampleAwait {
	public static final int REQUEST = 200;
	public static final int RESPONSE = 201;
	public static final int HELLO = 202;
	public static final int QUIT = 203;	// not STOP: Actor.STOP is the internal stop directive

	static class Client extends BehaviourActor {
		protected final ActorBehaviour idle = ActorBehaviour.named("IDLE", this::idle);

		protected final ActorId server;
		protected final CountDownLatch done;

		public Client(String name, ActorId server, CountDownLatch done) {
			super(name);
			this.server = server;
			this.done = done;
		}

		@Override
		protected ActorBehaviour initial() {
			return idle;
		}

		@Override
		public void postStop() {
			System.out.println(getName()+": stopped");
		}

		protected ActorBehaviour idle(ActorMessage<?> message) {
			switch (message.tag()) {
				case REQUEST -> {
					return request();
				}
				case HELLO -> {
					System.out.println(getName()+" ["+state()+"]: hello");
					return ActorBehaviour.same();
				}
				case QUIT -> {
					System.out.println(getName()+" ["+state()+"]: quit");
					return ActorBehaviour.stopped();
				}
				default -> {
					return ActorBehaviour.unhandled();
				}
			}
		}

		// the waiting behaviour is created per request (it holds the timer of the timeout)
		protected ActorBehaviour request() {
			tell(null, REQUEST, server);
			System.out.println(getName()+": request sent");

			// without otherwise: other messages go to idle (the current behaviour), QUIT stops and ends the waiting
			return ActorBehaviour.named("WAITING", await(RESPONSE, (message) -> {
				System.out.println(getName()+": response "+message.value());
				done.countDown();
				return idle;
			}, 1, TimeUnit.SECONDS, () -> {
				System.out.println(getName()+": timeout");
				done.countDown();
				return ActorBehaviour.initial();	// back to the start, without knowing its name
			}));
		}

		// the version of await with predicate and without timeout is used like this:
		// return await((message) -> message.tag()==RESPONSE, (message) -> idle);
	}

	static class Server extends BehaviourActor {
		public Server(String name) {
			super(name);
		}

		@Override
		protected ActorBehaviour initial() {
			return this::serve;
		}

		protected ActorBehaviour serve(ActorMessage<?> message) {
			if (message.tag()!=REQUEST)
				return ActorBehaviour.unhandled();
			tell("42", RESPONSE, message.source());
			return ActorBehaviour.same();
		}
	}

	public static void main(String[] args) throws InterruptedException {
		CountDownLatch done = new CountDownLatch(1);
		ActorSystem system = ActorRuntime.create();
		ActorId server = system.addActor(() -> new Server("server"));
		ActorId client = system.addActor(() -> new Client("client", server, done));
		system.start();

		system.send(ActorMessage.create(null, REQUEST, system.SYSTEM_ID(), client));
		system.send(ActorMessage.create(null, HELLO, system.SYSTEM_ID(), client)); // handled while waiting
		done.await(5, TimeUnit.SECONDS);
		
		// server without answer: timeout, back to the initial behaviour (ActorBehaviour.initial())
		CountDownLatch timeout = new CountDownLatch(1);
		ActorId silent = system.addActor(() -> new Server("silent") {
			@Override
			protected ActorBehaviour initial() {
				return ActorBehaviour.ignore();
			}
		});
		ActorId client2 = system.addActor(() -> new Client("client2", silent, timeout));
		system.send(ActorMessage.create(null, REQUEST, system.SYSTEM_ID(), client2));
		timeout.await(5, TimeUnit.SECONDS);
		system.send(ActorMessage.create(null, HELLO, system.SYSTEM_ID(), client2)); // IDLE again
		Thread.sleep(200);

		// QUIT while waiting: waiting ends and the client stops, no timeout afterwards
		ActorId client3 = system.addActor(() -> new Client("client3", silent, new CountDownLatch(1)));
		system.send(ActorMessage.create(null, REQUEST, system.SYSTEM_ID(), client3));
		system.send(ActorMessage.create(null, QUIT, system.SYSTEM_ID(), client3));
		Thread.sleep(1500);
		system.shutdownWithActors(true);
	}
}
