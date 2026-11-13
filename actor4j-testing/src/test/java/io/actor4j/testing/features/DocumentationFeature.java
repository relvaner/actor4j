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
import static io.actor4j.core.logging.ActorLogger.logger;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.actor4j.bdd.Story;
import io.actor4j.core.actors.Actor;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.testing.ActorTest;
import io.actor4j.testing.TestSystem;

// keeps the examples (documentation) compiling and passing
public class DocumentationFeature {
	public static class MyActor extends Actor implements ActorTest {
		@Override
		public void receive(ActorMessage<?> message) {
			if (message.value() instanceof String) {
				logger().info(String.format(
					"Received String message: %s", message.valueAsString()));
				tell("Hello World Again!", 0, message.source());
			}
			else
				unhandled(message);
		}

		@Override
		public List<Story> test() {
			TestSystem system = (TestSystem)getSystem();
			AtomicReference<String> actual = new AtomicReference<>();

			Story story = new Story();
			story.scenario()
				.annotate("Scenario: Sending and receiving a message String")
				.annotate("Given a request message")
				.given(() -> send(ActorMessage.create("Hello World!", 0, system.SYSTEM_ID(), self())))
				.annotate("When the responded message was received")
				.when(() -> {
					try {
						actual.set(system.awaitMessage(5, TimeUnit.SECONDS).valueAsString());
					} catch (InterruptedException | TimeoutException e) {
						throw new AssertionError("No response", e);
					}
				})
				.annotate("Then the responded received String message will be \"Hello World Again!\"")
				.then(() -> outcome(actual.get()).shouldBe("Hello World Again!"))
				.and(() -> system.assertNoMessages(200, TimeUnit.MILLISECONDS));

			return List.of(story);
		}
	}

	public static class CounterActor extends Actor implements ActorTest {
		public static final int INCREMENT = 1;
		public static final int STATE = 2;

		protected long counter;

		@Override
		public void receive(ActorMessage<?> message) {
			if (message.tag()==INCREMENT)
				counter++;
			else if (message.tag()==STATE)
				tell(counter, STATE, message.source());
		}

		@Override
		public List<Story> test() {
			TestSystem system = (TestSystem)getSystem();

			Story story = new Story();
			story.scenario()
				.given(() -> {
					for (int i=0; i<3; i++)
						send(ActorMessage.create(null, INCREMENT, system.SYSTEM_ID(), self()));
				})
				.when(() -> send(ActorMessage.create(null, STATE, system.SYSTEM_ID(), self())))
				.then(() -> {
					try {
						outcome(system.awaitMessage(5, TimeUnit.SECONDS).value()).shouldBe(3L);
					} catch (InterruptedException | TimeoutException e) {
						throw new AssertionError("No state", e);
					}
				});

			return List.of(story);
		}
	}

	protected TestSystem system;

	@Before
	public void before() {
		system = TestSystem.create();
	}

	@After
	public void after() {
		system.shutdownWithActors(true);
	}

	@Test(timeout=5000)
	public void test_my_actor() {
		system.addActor(() -> new MyActor());
		system.start();

		system.testAllActors();
	}

	@Test(timeout=5000)
	public void test_counter_actor() {
		system.addActor(() -> new CounterActor());
		system.start();

		system.testAllActors();
	}
}
