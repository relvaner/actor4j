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

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.actor4j.bdd.Story;
import io.actor4j.core.actors.Actor;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.testing.TestSystem;
import io.actor4j.testing.config.TestSystemConfig;

public class ActorTestFeature {
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

	protected void awaitPendingMessage() throws InterruptedException {
		for (int i=0; i<200; i++) {
			system.assertNoMessages();
			Thread.sleep(10);
		}
	}

	@Test(timeout=5000)
	public void test_reply_received() {
		ResponderActor actor = new ResponderActor(1);
		actor.step(() -> actor.request())
			.step(() -> outcome(awaitValue()).shouldBe("reply0"))
			.step(() -> system.assertNoMessages());
		system.addActor(() -> actor);
		system.start();

		system.testAllActors();
	}

	@Test(timeout=5000)
	public void test_failed_assertion_is_thrown() {
		ResponderActor actor = new ResponderActor(0);
		actor.step(() -> outcome("a").shouldBe("b"))
			.nextStory()
			.step(() -> outcome("c").shouldBe("d"));
		system.addActor(() -> actor);
		system.start();

		try {
			system.testAllActors();
			fail("AssertionError expected");
		}
		catch (AssertionError e) {
			assertTrue(e.getMessage().contains("b"));
			assertEquals(1, e.getSuppressed().length);
			assertTrue(e.getSuppressed()[0].getMessage().contains("d"));
		}
	}

	@Test(timeout=5000)
	public void test_failed_assertion_via_test_actor() {
		ResponderActor actor = new ResponderActor(0);
		actor.step(() -> outcome("a").shouldBe("b"));
		ActorId id = system.addActor(() -> actor);
		system.start();

		assertThrows(AssertionError.class, () -> system.testActor(id));
	}

	@Test(timeout=5000)
	public void test_redirection_ends_after_failed_step() throws InterruptedException {
		CountDownLatch received = new CountDownLatch(1);
		ResponderActor actor = new ResponderActor(0);
		actor.step(() -> { throw new IllegalStateException("step failed"); });
		system.addActor(() -> actor);
		ActorId other = system.addActor(() -> new Actor() {
			@Override
			public void receive(ActorMessage<?> message) {
				received.countDown();
			}
		});
		system.start();

		assertThrows(IllegalStateException.class, () -> system.testAllActors());

		system.send(ActorMessage.create("hello", 0, system.SYSTEM_ID(), other));
		assertTrue(received.await(2, TimeUnit.SECONDS));
	}

	@Test(timeout=5000)
	public void test_redirection_ends_after_failed_test_method() throws InterruptedException {
		CountDownLatch received = new CountDownLatch(1);
		system.addActor(() -> new ResponderActor(0) {
			@Override
			public List<Story> test() {
				throw new IllegalStateException("test() failed");
			}
		});
		ActorId other = system.addActor(() -> new Actor() {
			@Override
			public void receive(ActorMessage<?> message) {
				received.countDown();
			}
		});
		system.start();

		assertThrows(IllegalStateException.class, () -> system.testAllActors());

		system.send(ActorMessage.create("hello", 0, system.SYSTEM_ID(), other));
		assertTrue(received.await(2, TimeUnit.SECONDS));
	}

	@Test(timeout=5000)
	public void test_assert_no_messages_with_timeout_detects_late_message() {
		ResponderActor actor = new ResponderActor(1, 200);
		actor.step(() -> actor.request())
			.step(() -> system.assertNoMessages())
			.step(() -> assertThrows(AssertionError.class, () -> system.assertNoMessages(1, TimeUnit.SECONDS)))
			.step(() -> outcome(awaitValue()).shouldBe("reply0"));
		system.addActor(() -> actor);
		system.start();

		system.testAllActors();
	}

	@Test(timeout=5000)
	public void test_assert_no_messages_with_timeout_waits() {
		ResponderActor actor = new ResponderActor(0);
		actor.step(() -> {
			long start = System.nanoTime();
			system.assertNoMessages(300, TimeUnit.MILLISECONDS);
			assertTrue(System.nanoTime()-start>=TimeUnit.MILLISECONDS.toNanos(300));
		});
		system.addActor(() -> actor);
		system.start();

		system.testAllActors();
	}

	@Test(timeout=5000)
	public void test_assert_no_messages_does_not_consume() {
		ResponderActor actor = new ResponderActor(2);
		actor.step(() -> actor.request())
			.step(() -> outcome(awaitValue()).shouldBe("reply0"))
			.step(() -> assertThrows(AssertionError.class, () -> awaitPendingMessage()))
			.step(() -> outcome(awaitValue()).shouldBe("reply1"))
			.step(() -> system.assertNoMessages());
		system.addActor(() -> actor);
		system.start();

		system.testAllActors();
	}
}
