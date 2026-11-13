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
package io.actor4j.testing.features;

import static io.actor4j.bdd.OutcomeFactory.outcome;
import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.actor4j.core.actors.Actor;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.runtime.InternalActorSystem;
import io.actor4j.core.runtime.protocols.ActorProtocolTag;
import io.actor4j.testing.TestSystem;
import io.actor4j.testing.config.TestSystemConfig;

public class RedirectFeature {
	protected TestSystem system;

	@Before
	public void before() {
		system = TestSystem.create(TestSystemConfig.builder().parallelism(2).build());
	}

	@After
	public void after() {
		system.shutdownWithActors(true);
	}

	protected String awaitValue() {
		try {
			return system.awaitMessage(2, TimeUnit.SECONDS).valueAsString();
		} catch (InterruptedException | TimeoutException e) {
			return null;
		}
	}

	protected ActorId addReceiver(CountDownLatch received) {
		return system.addActor(() -> new Actor() {
			@Override
			public void receive(ActorMessage<?> message) {
				received.countDown();
			}
		});
	}

	@Test(timeout=5000)
	public void test_sends_of_test_actor_redirected() {
		CountDownLatch received = new CountDownLatch(1);
		AtomicReference<ActorId> other = new AtomicReference<>();
		ResponderActor actor = new ResponderActor(0);
		actor.step(() -> {
				InternalActorSystem internal = (InternalActorSystem)system;
				actor.tell("via-id", 0, other.get());
				actor.tell("via-alias", 0, "other");
				internal.getMessageDispatcher().unsafe_post(ActorMessage.create("unsafe-id", 0, actor.self(), other.get()), actor.self());
				internal.getMessageDispatcher().unsafe_post(ActorMessage.create("unsafe-alias", 0, actor.self(), null), actor.self(), "other");
				actor.priority("priority-id", 0, other.get());
			})
			.step(() -> outcome(awaitValue()).shouldBe("via-id"))
			.step(() -> outcome(awaitValue()).shouldBe("via-alias"))
			.step(() -> outcome(awaitValue()).shouldBe("unsafe-id"))
			.step(() -> outcome(awaitValue()).shouldBe("unsafe-alias"))
			.step(() -> outcome(awaitValue()).shouldBe("priority-id"))
			.step(() -> outcome(received.getCount()).shouldBe(1));
		system.addActor(() -> actor);
		other.set(addReceiver(received));
		system.setAlias(other.get(), "other");
		system.start();

		system.testAllActors();
	}

	@Test(timeout=5000)
	public void test_unrelated_messages_delivered() {
		CountDownLatch received = new CountDownLatch(1);
		AtomicReference<ActorId> other = new AtomicReference<>();
		ResponderActor actor = new ResponderActor(0);
		actor.step(() -> system.send(ActorMessage.create("unrelated", 0, system.SYSTEM_ID(), other.get())))
			.step(() -> {
				try {
					outcome(received.await(2, TimeUnit.SECONDS)).shouldBeTrue();
				} catch (InterruptedException e) {
					fail();
				}
			})
			.step(() -> system.assertNoMessages());
		system.addActor(() -> actor);
		other.set(addReceiver(received));
		system.start();

		system.testAllActors();
	}

	@Test(timeout=5000)
	public void test_directives_of_test_actor_delivered() {
		CountDownLatch stopped = new CountDownLatch(1);
		AtomicReference<ActorId> child = new AtomicReference<>();
		ResponderActor actor = new ResponderActor(0) {
			@Override
			public void preStart() {
				child.set(addChild(() -> new Actor() {
					@Override
					public void receive(ActorMessage<?> message) {
					}

					@Override
					public void postStop() {
						stopped.countDown();
					}
				}));
			}
		};
		actor.step(() -> ((InternalActorSystem)system).sendAsDirective(ActorMessage.create(null, ActorProtocolTag.INTERNAL_STOP, actor.self(), child.get())))
			.step(() -> {
				try {
					outcome(stopped.await(2, TimeUnit.SECONDS)).shouldBeTrue();
				} catch (InterruptedException e) {
					fail();
				}
			});
		system.addActor(() -> actor);
		system.start();

		system.testAllActors();
	}
}
