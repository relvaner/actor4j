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
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.pods.PodConfiguration;
import io.actor4j.data.publish.subscribe.BrokerActor;
import io.actor4j.data.publish.subscribe.Publish;
import io.actor4j.data.publish.subscribe.Subscribe;

import static io.actor4j.core.logging.ActorLogger.*;
import static org.junit.Assert.*;

public class PolyglotStreamsFeature {
	protected ActorSystem system;
	
	public static ActorSystemFactory factory() {
		return ActorRuntime.factory();
	}

	@Before
	public void before() {
		system = ActorSystem.create(factory(), ActorSystemConfig.builder().serverMode().parallelism(4).build());
	}
	
	@Test(timeout=5000)
	public void test_factory_ExamplePolyglotFunctionPod_JS_PubSub() {
		CountDownLatch testDone = new CountDownLatch(2);
		
		String domain = "ExamplePolyglotFunctionPod_JS6";
		
		ActorId broker = system.addActor(() -> new BrokerActor());
		system.setAlias(broker, "broker");
		
		system.deployPods(
				() -> new ExamplePolyglotFunctionPod_JS6(), 
				new PodConfiguration(domain, ExamplePolyglotFunctionPod_JS6.class.getName(), 1, 1));
		ActorId subscriber = system.addActor(() -> new Actor(){
			@Override
			public void receive(ActorMessage<?> message) {
				logger().log(DEBUG, String.format("subscriber received a message ('%s') from "+ domain, message.value()));
				
				assertTrue(message.value() instanceof Publish);
				if (message.value() instanceof Publish p) {
					assertTrue(p.topic().equalsIgnoreCase("MyTopic_out"));
					assertTrue(p.value() instanceof Integer);
					if (p.value() instanceof Integer v)
						assertEquals(42+1+2-testDone.getCount(), v.longValue());
				}
				testDone.countDown();
			}
		});
		system.start();
		
		system.send(ActorMessage.create(new Subscribe("MyTopic_out"), 0, subscriber, broker));

		system.send(ActorMessage.create(new Publish<Integer>("MyTopic_in", 1), 0, null, broker));
		system.send(ActorMessage.create(new Publish<Integer>("MyTopic_in", 2), 0, null, broker));
		
		try {
			testDone.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		system.shutdownWithActors(true);
	}
}
