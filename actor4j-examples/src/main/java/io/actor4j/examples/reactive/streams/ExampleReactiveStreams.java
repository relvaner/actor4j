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
package io.actor4j.examples.reactive.streams;

import static io.actor4j.core.logging.ActorLogger.DEBUG;
import static io.actor4j.core.logging.ActorLogger.logger;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.actor4j.core.ActorSystem;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.reactive.streams.PublisherActor;
import io.actor4j.core.reactive.streams.SubscriberActor;
import io.actor4j.examples.shared.ExamplesSettings;

public class ExampleReactiveStreams {
	public ExampleReactiveStreams() {
		ActorSystem system = ActorSystem.create(ExamplesSettings.factory());
		
		ActorId publisher = system.addActor(() -> new PublisherActor("publisher") {
			@Override
			public void receive(ActorMessage<?> message) {
				super.receive(message);
				if (message.tag()==1001)
					broadcast(message.value());
			}
		});
		
		system.addActor(() -> new SubscriberActor("subscriberA") {
			@Override
			public void preStart() {
				subscribe(publisher, (obj) -> logger().log(DEBUG, String.format("Actor (%s) has received: %s", name, obj)), null, null);
				request(10, publisher);
			}
		});
		system.addActor(() -> new SubscriberActor("subscriberB") {
			@Override
			public void preStart() {
				subscribe(publisher, (obj) -> logger().log(DEBUG, String.format("Actor (%s) has received: %s", name, obj)), null, null);
				request(15, publisher);
			}
		});
		
		system.start();
		
		Random random = new Random();
		system.timer().schedule(() -> ActorMessage.create(random.nextInt(512), 1001, null, null), publisher, 500, 100, TimeUnit.MILLISECONDS);

		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		system.shutdownWithActors(true);
	}
	
	public static void main(String[] args) {
		new ExampleReactiveStreams();
	}
}
