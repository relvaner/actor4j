/*
 * Copyright (c) 2015-2018, David A. Bauer. All rights reserved.
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
package io.actor4j.core.data.access.features;

import static io.actor4j.core.logging.ActorLogger.*;
import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.actor4j.core.ActorSystem;
import io.actor4j.core.actors.Actor;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorOptional;
import io.actor4j.core.publish.subscribe.BrokerActor;
import io.actor4j.core.publish.subscribe.Publish;
import io.actor4j.core.publish.subscribe.Subscribe;
import io.actor4j.core.publish.subscribe.utils.PubSubActorManager;

public class PubSubFeature {
	@Test(timeout=5000)
	public void test_pub_sub() {
		ActorSystem system = ActorSystem.create(AllFeaturesTest.factory());
		
		CountDownLatch testDone = new CountDownLatch(2);
		
		final int[] values = new int[] { -1, 341, 351, 451, 318, 292, 481, 240, 478, 382, 502, 158, 401, 438, 353, 165, 344, 6, 9, 18, 31, 77, 90, 45, 63, 190, 1 };
		ActorId broker = system.addActor(() -> new BrokerActor());
		
		ActorId subscriberA = system.addActor(() -> new Actor("subscriberA") {
			protected int i = 0;
			@Override
			public void receive(ActorMessage<?> message) {
				logger().log(DEBUG, String.format("Message received (%s): %s", name, ((Publish<?>)message.value()).value()));
				assertEquals(values[i], ((Publish<?>)message.value()).value());
				i++;
				if (i==values.length)
					testDone.countDown();
			}
		});
		ActorId subscriberB = system.addActor(() -> new Actor("subscriberB") {
			protected int i = 0;
			@Override
			public void receive(ActorMessage<?> message) {
				logger().log(DEBUG, String.format("Message received (%s): %s", name, ((Publish<?>)message.value()).value()));
				assertEquals(values[i], ((Publish<?>)message.value()).value());
				i++;
				if (i==values.length)
					testDone.countDown();
			}
		});
		
		system.send(ActorMessage.create(new Subscribe("MyTopic"), 0, subscriberA, broker));
		system.send(ActorMessage.create(new Subscribe("MyTopic"), 0, subscriberB, broker));
		
		system.addActor(() -> new Actor("publisher") {
			protected int i = 1;
			@Override
			public void preStart() {
				send(ActorMessage.create(new Publish<Integer>("MyTopic", -1), BrokerActor.GET_TOPIC_ACTOR, self(), broker));
			}
			
			@Override
			public void receive(ActorMessage<?> message) {
				if (message.tag()==BrokerActor.GET_TOPIC_ACTOR)
					system.timer().schedule(() -> {
						if (i<values.length)
							return ActorMessage.create(new Publish<Integer>("MyTopic", values[i++]), 0, null, null);
						else
							throw new RuntimeException("Task canceled");
					}, message.valueAsId(), 0, 25, TimeUnit.MILLISECONDS);
			}
		});
		
		system.start();
		
		try {
			testDone.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		system.shutdownWithActors(true);
	}
	
	@Test(timeout=5000)
	public void test_pub_sub_with_manager() {
		ActorSystem system = ActorSystem.create(AllFeaturesTest.factory());
		
		CountDownLatch testDone = new CountDownLatch(2);
		
		final int[] values = new int[] { -1, 341, 351, 451, 318, 292, 481, 240, 478, 382, 502, 158, 401, 438, 353, 165, 344, 6, 9, 18, 31, 77, 90, 45, 63, 190, 1 };
		ActorId broker = system.addActor(PubSubActorManager.createBrokerWithFactory());
		
		system.addActor(() -> new Actor("subscriberA") {
			protected PubSubActorManager<Integer> manager = new PubSubActorManager<>(this, broker);
			protected int i = 0;
			@Override
			public void preStart() {
				manager.subscribe("MyTopic");
			}
			@Override
			public void receive(ActorMessage<?> message) {
				ActorOptional<Integer> optional = manager.get(message);
				assertTrue(optional.isDone());
				assertEquals(values[i], (int)optional.get());
				logger().log(DEBUG, String.format("Message received (%s): %d", name, optional.get()));
				i++;
				if (i==values.length)
					testDone.countDown();
			}
		});
		system.addActor(() -> new Actor("subscriberB") {
			protected PubSubActorManager<Integer> manager = new PubSubActorManager<>(this, broker);
			protected int i = 0;
			@Override
			public void preStart() {
				manager.subscribe("MyTopic");
			}
			@Override
			public void receive(ActorMessage<?> message) {
				ActorOptional<Integer> optional = manager.get(message);
				assertTrue(optional.isDone());
				assertTrue(optional.isPresent());
				assertEquals(values[i], (int)optional.get());
				logger().log(DEBUG, String.format("Message received (%s): %d", name, optional.get()));
				i++;
				if (i==values.length)
					testDone.countDown();
			}
		});
		
		ActorId publisher = system.addActor(() -> new Actor("publisher") {
			protected PubSubActorManager<Integer> manager = new PubSubActorManager<>(this, broker);
			protected int i = 1;
			@Override
			public void receive(ActorMessage<?> message) {
				if (message.source()==getSystem().SYSTEM_ID())
					manager.publish(new Publish<Integer>("MyTopic", -1));
				
				ActorOptional<ActorId> optional = manager.getTopic(message);
				if (optional.isDone())
					system.timer().schedule(() -> {
						if (i<values.length)
							return ActorMessage.create(new Publish<Integer>("MyTopic", values[i++]), 0, null, null);
						else
							throw new RuntimeException("Task canceled");
					}, optional.get(), 0, 25, TimeUnit.MILLISECONDS);
			}
		});
		
		system.start();
		system.send(ActorMessage.create(null, 0, system.SYSTEM_ID(), publisher));
		
		try {
			testDone.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		system.shutdownWithActors(true);
	}
}
