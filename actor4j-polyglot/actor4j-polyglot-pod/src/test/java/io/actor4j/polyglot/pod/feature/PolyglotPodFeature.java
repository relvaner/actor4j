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
package io.actor4j.polyglot.pod.feature;

import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Test;

import io.actor4j.core.ActorRuntime;
import io.actor4j.core.ActorSystem;
import io.actor4j.core.ActorSystemFactory;
import io.actor4j.core.actors.Actor;
import io.actor4j.core.config.ActorSystemConfig;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.json.JsonArray;
import io.actor4j.core.json.JsonObject;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.pods.PodConfiguration;

import static io.actor4j.core.logging.ActorLogger.*;
import static org.junit.Assert.*;

public class PolyglotPodFeature {
	protected ActorSystem system;
	
	public static ActorSystemFactory factory() {
		return ActorRuntime.factory();
	}

	@Before
	public void before() {
		system = ActorSystem.create(factory(), ActorSystemConfig.builder().serverMode().parallelism(4).build());
	}

	@Test(timeout=5000)
	public void test_factory_ExamplePolyglotFunctionPod_JS() {
		CountDownLatch testDone = new CountDownLatch(1);
		
		system.deployPods(
				() -> new ExamplePolyglotFunctionPod_JS(), 
				new PodConfiguration("ExamplePolyglotFunctionPod_JS", ExamplePolyglotFunctionPod_JS.class.getName(), 1, 1));
		ActorId client = system.addActor(() -> new Actor(){
			@Override
			public void receive(ActorMessage<?> message) {
				logger().log(DEBUG, String.format("client received a message ('%s') from ExamplePolyglotFunctionPod_JS", message.valueAsJsonObject()));
				
				assertEquals(42, message.tag());
				assertTrue(message.value()!=null);
				assertTrue(message.value() instanceof JsonObject);
				if (message.value() instanceof JsonObject obj)
					assertTrue(obj.getString("value").startsWith("Hello Test!"));
				testDone.countDown();
			}
		});
		system.start();
		
		system.sendViaAlias(ActorMessage.create("Test", 0, client, null), "ExamplePolyglotFunctionPod_JS");
		
		try {
			testDone.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		system.shutdownWithActors(true);
	}
	
	@Test(timeout=5000)
	public void test_factory_ExamplePolyglotFunctionPod_JS_WarmUp() {
		test_factory_ExamplePolyglotFunctionPod_JS();
	}
	
	@Test(timeout=5000)
	public void test_factory_ExamplePolyglotFunctionPod_PY() {
		CountDownLatch testDone = new CountDownLatch(1);
		
		system.deployPods(
				() -> new ExamplePolyglotFunctionPod_PY(), 
				new PodConfiguration("ExamplePolyglotFunctionPod_PY", ExamplePolyglotFunctionPod_PY.class.getName(), 1, 1));
		ActorId client = system.addActor(() -> new Actor(){
			@Override
			public void receive(ActorMessage<?> message) {
				logger().log(DEBUG, String.format("client received a message ('%s') from ExamplePolyglotFunctionPod_PY", message.valueAsJsonObject()));
				
				assertEquals(42, message.tag());
				assertTrue(message.value()!=null);
				assertTrue(message.value() instanceof JsonObject);
				if (message.value() instanceof JsonObject obj)
					assertTrue(obj.getString("value").startsWith("Hello Test!"));
				testDone.countDown();
			}
		});
		system.start();
		
		system.sendViaAlias(ActorMessage.create("Test", 0, client, null), "ExamplePolyglotFunctionPod_PY");
		
		try {
			testDone.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		system.shutdownWithActors(true);
	}
	
	@Test(timeout=5000)
	public void test_factory_ExamplePolyglotFunctionPod_PY_WarmUp() {
		test_factory_ExamplePolyglotFunctionPod_PY();
	}
	
	@Test(timeout=5000)
	public void test_factory_ExamplePolyglotFunctionPod_JS2() {
		CountDownLatch testDone = new CountDownLatch(2);
		
		system.deployPods(
				() -> new ExamplePolyglotFunctionPod_JS2(), 
				new PodConfiguration("ExamplePolyglotFunctionPod_JS2", ExamplePolyglotFunctionPod_JS2.class.getName(), 1, 1));
		ActorId client = system.addActor(() -> new Actor(){
			@Override
			public void receive(ActorMessage<?> message) {
				logger().log(DEBUG, String.format("client received a message ('%s') from ExamplePolyglotFunctionPod_JS2", message.valueAsJsonObject()));
				
				assertEquals(42, message.tag());
				assertTrue(message.value()!=null);
				assertTrue(message.value() instanceof JsonObject);
				if (message.value() instanceof JsonObject obj)
					assertTrue(obj.getString("value").startsWith("Hello Test!"));
				testDone.countDown();
			}
		});
		ActorId other = system.addActor(() -> new Actor(){
			@Override
			public void preStart() {
				expose();
			}
			
			@Override
			public void receive(ActorMessage<?> message) {
				logger().log(DEBUG, String.format("client received a message ('%s') from ExamplePolyglotFunctionPod_JS2", message.valueAsJsonObject()));
				
				assertEquals(423, message.tag());
				assertTrue(message.value()!=null);
				assertTrue(message.value() instanceof JsonObject);
				if (message.value() instanceof JsonObject obj)
					assertTrue(obj.getString("value").startsWith("Hello Test2!"));
					
				testDone.countDown();
			}
		});
		system.start();
		
		system.sendViaAlias(ActorMessage.create(other, 0, client, null), "ExamplePolyglotFunctionPod_JS2");
		
		try {
			testDone.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		system.shutdownWithActors(true);
	}
	
	@Test(timeout=5000)
	public void test_factory_ExamplePolyglotFunctionPod_PY2() {
		CountDownLatch testDone = new CountDownLatch(1);
		
		system.deployPods(
				() -> new ExamplePolyglotFunctionPod_PY2(), 
				new PodConfiguration("ExamplePolyglotFunctionPod_PY2", ExamplePolyglotFunctionPod_PY2.class.getName(), 1, 1));
		ActorId client = system.addActor(() -> new Actor(){
			@Override
			public void receive(ActorMessage<?> message) {
				logger().log(DEBUG, String.format("client received a message ('%d') from ExamplePolyglotFunctionPod_PY2", message.valueAsInt()));
				
				assertEquals(0, message.tag());
				assertTrue(message.valueAsInt()==67);
				testDone.countDown();
			}
		});
		system.start();
		
		system.sendViaAlias(ActorMessage.create("Test", 0, client, null), "ExamplePolyglotFunctionPod_PY2");
		
		try {
			testDone.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		system.shutdownWithActors(true);
	}
	
	@Test(timeout=5000)
	public void test_factory_ExamplePolyglotFunctionPod_PY3() {
		CountDownLatch testDone = new CountDownLatch(1);
		
		system.deployPods(
				() -> new ExamplePolyglotFunctionPod_PY3(), 
				new PodConfiguration("ExamplePolyglotFunctionPod_PY3", ExamplePolyglotFunctionPod_PY3.class.getName(), 1, 1));
		ActorId client = system.addActor(() -> new Actor(){
			@Override
			public void receive(ActorMessage<?> message) {
				logger().log(DEBUG, String.format("client received a message ('%s') from ExamplePolyglotFunctionPod_PY3", message.valueAsJsonArray()));
				
				assertEquals(0, message.tag());
				assertTrue(message.value()!=null);
				assertTrue(message.value() instanceof JsonArray);
				if (message.value() instanceof JsonArray arr)
					assertTrue(arr.getString(0).startsWith("value"));
				testDone.countDown();
			}
		});
		system.start();
		
		system.sendViaAlias(ActorMessage.create("Test", 0, client, null), "ExamplePolyglotFunctionPod_PY3");
		
		try {
			testDone.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		system.shutdownWithActors(true);
	}
	
	@Test(timeout=5000)
	public void test_factory_ExamplePolyglotFunctionPod_JS3() {
		CountDownLatch testDone = new CountDownLatch(1);
		
		system.deployPods(
				() -> new ExamplePolyglotFunctionPod_JS3(), 
				new PodConfiguration("ExamplePolyglotFunctionPod_JS3", ExamplePolyglotFunctionPod_JS3.class.getName(), 1, 1));
		ActorId client = system.addActor(() -> new Actor(){
			@Override
			public void receive(ActorMessage<?> message) {
				logger().log(DEBUG, String.format("client received a message ('%s') from ExamplePolyglotFunctionPod_JS3", message.valueAsJsonArray()));
				
				assertEquals(0, message.tag());
				assertTrue(message.value()!=null);
				assertTrue(message.value() instanceof JsonArray);
				if (message.value() instanceof JsonArray arr)
					assertTrue(arr.getString(0).startsWith("value"));
				testDone.countDown();
			}
		});
		system.start();
		
		system.sendViaAlias(ActorMessage.create("Test", 0, client, null), "ExamplePolyglotFunctionPod_JS3");
		
		try {
			testDone.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		system.shutdownWithActors(true);
	}
}
