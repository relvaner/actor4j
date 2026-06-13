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
package io.actor4j.polyglot.pods.feature;

import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Test;

import io.actor4j.core.ActorRuntime;
import io.actor4j.core.ActorSystem;
import io.actor4j.core.ActorSystemFactory;
import io.actor4j.core.actors.Actor;
import io.actor4j.core.config.ActorSystemConfig;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.json.JsonObject;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.pods.PodConfiguration;

import static io.actor4j.core.logging.ActorLogger.*;
import static org.junit.Assert.*;

public class PolyglotPodFeatureJava {
	protected ActorSystem system;
	
	public static ActorSystemFactory factory() {
		return ActorRuntime.factory();
	}

	@Before
	public void before() {
		system = ActorSystem.create(factory(), ActorSystemConfig.builder().serverMode().parallelism(4).build());
	}

	@Test(timeout=5000)
	public void test_factory_ExamplePolyglotFunctionPod_JAVA() {
		CountDownLatch testDone = new CountDownLatch(1);
		
		system.deployPods(
				() -> new ExamplePolyglotFunctionPod_JAVA(), 
				new PodConfiguration("ExamplePolyglotFunctionPod_JAVA", ExamplePolyglotFunctionPod_JAVA.class.getName(), 1, 1));
		ActorId client = system.addActor(() -> new Actor(){
			@Override
			public void receive(ActorMessage<?> message) {
				logger().log(DEBUG, String.format("client received a message ('%s') from ExamplePolyglotFunctionPod_JAVA", message.valueAsJsonObject()));
				
				assertEquals(42, message.tag());
				assertTrue(message.value()!=null);
				assertTrue(message.value() instanceof JsonObject);
				if (message.value() instanceof JsonObject obj)
					assertTrue(obj.getString("value").startsWith("Hello Test!"));
				testDone.countDown();
			}
		});
		system.start();
		
		system.sendViaAlias(ActorMessage.create("Test", 0, client, null), "ExamplePolyglotFunctionPod_JAVA");
		
		try {
			testDone.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		system.shutdownWithActors(true);
	}
}
