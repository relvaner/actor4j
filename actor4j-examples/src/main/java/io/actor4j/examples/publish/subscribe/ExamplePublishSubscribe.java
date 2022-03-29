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
package io.actor4j.examples.publish.subscribe;

import static io.actor4j.core.logging.ActorLogger.DEBUG;
import static io.actor4j.core.logging.ActorLogger.logger;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.actor4j.core.ActorSystem;
import io.actor4j.core.actors.Actor;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.publish.subscribe.BrokerActor;
import io.actor4j.core.publish.subscribe.Publish;
import io.actor4j.core.publish.subscribe.Subscribe;

public class ExamplePublishSubscribe {
	public ExamplePublishSubscribe() {
		ActorSystem system = ActorSystem.create();
		
		UUID broker = system.addActor(() -> new BrokerActor());
		
		UUID subscriberA = system.addActor(() -> new Actor("subscriberA") {
			@Override
			public void receive(ActorMessage<?> message) {
				logger().log(DEBUG, String.format("Message received (%s): %s", name, ((Publish<?>)message.value()).value()));
			}
		});
		UUID subscriberB = system.addActor(() -> new Actor("subscriberB") {
			@Override
			public void receive(ActorMessage<?> message) {
				logger().log(DEBUG, String.format("Message received (%s): %s", name, ((Publish<?>)message.value()).value()));
			}
		});
		
		system.send(ActorMessage.create(new Subscribe("MyTopic"), 0, subscriberA, broker));
		system.send(ActorMessage.create(new Subscribe("MyTopic"), 0, subscriberB, broker));
		
		system.addActor(() -> new Actor("publisher") {
			protected Random random;
			@Override
			public void preStart() {
				random = new Random();
				send(ActorMessage.create(new Publish<String>("MyTopic", String.valueOf(random.nextInt(512))), BrokerActor.GET_TOPIC_ACTOR, self(), broker));
			}
			
			@Override
			public void receive(ActorMessage<?> message) {
				if (message.tag()==BrokerActor.GET_TOPIC_ACTOR) { 
					system.timer().schedule(() -> ActorMessage.create(new Publish<String>("MyTopic", String.valueOf(random.nextInt(512))), 0, null, null), message.valueAsUUID(), 0, 100, TimeUnit.MILLISECONDS);
				}
			}
		});
		
		/*
		Random random = new Random();
		system.timer().schedule(() -> new ActorMessage<Publish<String>>(new Publish<String>("MyTopic", String.valueOf(random.nextInt(512))), 0, null, null), broker, 0, 100);
		*/
		system.start();
		
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		system.shutdownWithActors(true);
	}
	
	public static void main(String[] args) {
		new ExamplePublishSubscribe();
	}
}
