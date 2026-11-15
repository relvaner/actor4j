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

// ping-pong with behaviours: each state is a method, kept once as named field, switch by returning it
public class ExamplePingPong {
	public static final int PING = 100;
	public static final int PONG = 101;
	public static final int PAUSE = 102;
	public static final int RESUME = 103;

	static class Ping extends BehaviourActor {
		protected final ActorBehaviour ping = ActorBehaviour.named("PING", this::ping);
		protected final ActorBehaviour paused = ActorBehaviour.named("PAUSED", this::paused);

		protected final CountDownLatch done;
		protected int rounds;

		public Ping(String name, CountDownLatch done) {
			super(name);
			this.done = done;
		}

		@Override
		protected ActorBehaviour initial() {
			return ping;
		}

		@Override
		public void postStop() {
			System.out.println("ping: stopped");
		}

		protected ActorBehaviour ping(ActorMessage<?> message) {
			switch (message.tag()) {
				case PONG -> {
					System.out.println("ping ["+state()+"]: round "+(++rounds));
					if (rounds==6) {
						done.countDown();
						return ActorBehaviour.stopped();
					}
					if (rounds==3)
						send(ActorMessage.create(null, PAUSE, self(), self())); // pause after round 3
					tell(null, PING, message.source());
					return ActorBehaviour.same();
				}
				case PAUSE -> {
					return paused;
				}
				default -> {
					return ActorBehaviour.unhandled();
				}
			}
		}

		protected ActorBehaviour paused(ActorMessage<?> message) {
			switch (message.tag()) {
				case PONG -> {
					System.out.println("ping ["+state()+"]: pong ignored, resume");
					send(ActorMessage.create(null, RESUME, self(), self()));
					send(ActorMessage.create(null, PONG, message.source(), self())); // replay the pong
					return ActorBehaviour.same();
				}
				case RESUME -> {
					return ping;
				}
				default -> {
					return ActorBehaviour.unhandled();
				}
			}
		}
	}

	static class Pong extends BehaviourActor {
		protected final ActorBehaviour pong = ActorBehaviour.named("PONG", this::pong);

		public Pong(String name) {
			super(name);
		}

		@Override
		protected ActorBehaviour initial() {
			return pong;
		}

		protected ActorBehaviour pong(ActorMessage<?> message) {
			if (message.tag()!=PING)
				return ActorBehaviour.unhandled();
			tell(null, PONG, message.source());
			return ActorBehaviour.same();
		}
	}

	public static void main(String[] args) throws InterruptedException {
		CountDownLatch done = new CountDownLatch(1);
		ActorSystem system = ActorRuntime.create();
		ActorId ping = system.addActor(() -> new Ping("ping", done));
		ActorId pong = system.addActor(() -> new Pong("pong"));
		system.start();

		system.send(ActorMessage.create(null, PONG, pong, ping)); // as if pong had sent it
		done.await(5, TimeUnit.SECONDS);
		system.shutdownWithActors(true);
	}
}
