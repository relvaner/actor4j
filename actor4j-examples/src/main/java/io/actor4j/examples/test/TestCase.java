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
package io.actor4j.examples.test;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.actor4j.testing.ActorTest;
import io.actor4j.testing.TestSystem;
import io.actor4j.bdd.Story;
import io.actor4j.core.actors.Actor;
import io.actor4j.core.messages.ActorMessage;

import static io.actor4j.bdd.OutcomeFactory.*;
import static io.actor4j.core.logging.ActorLogger.logger;

public class TestCase {
	protected static class MyActor extends Actor implements ActorTest {
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
			List<Story> result = new LinkedList<>();
			
			MutableObject<String> actual = new MutableObject<>();
			
			Story story = new Story();
			story.scenario()
				.annotate("Sceneario: Sending and receiving a message String")
				.annotate("Given a request message")
				.given(() -> {
					send(ActorMessage.create("Hello World!", 0, getSystem().SYSTEM_ID, self()));
				})
				.annotate("When the responded message was received")
				.when(()-> {
					try {
						ActorMessage<?> message = ((TestSystem)getSystem()).awaitMessage(5, TimeUnit.SECONDS);
						actual.setValue(message.valueAsString());
					} catch (InterruptedException | TimeoutException e) {
						e.printStackTrace();
					}
					
					((TestSystem)getSystem()).assertNoMessages();
				})
				.annotate("Then the responded received String message will be \"Hello World Again!\"")
				.then(() -> {
					outcome(actual.getValue()).shouldBe("Hello World Again!");
				});
			
			result.add(story);
			
			return result;
		}
	}
	
	protected TestSystem system;
	
	@Before
	public void before() {
		system = new TestSystem();
		
		system.addActor(() -> new MyActor());

		system.start();
	}
	
	@Test
	public void test() {
		system.testAllActors();
	}
	
	@After
	public void after() {
		system.shutdownWithActors(true);
	}
	
	public static void main(String... args) {
		TestCase testCase = new TestCase();
		testCase.before();
		testCase.test();
		testCase.after();
	}
}
