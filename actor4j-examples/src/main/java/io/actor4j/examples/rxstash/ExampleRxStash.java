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
package io.actor4j.examples.rxstash;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.actor4j.core.ActorSystem;
import io.actor4j.core.actors.Actor;
import io.actor4j.corex.actors.ActorWithRxStash;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorFactory;
import io.actor4j.core.utils.ActorMessageMatcher;

public class ExampleRxStash {
	public ExampleRxStash() {
		ActorSystem system = new ActorSystem();
		
		UUID receiver = system.addActor(new ActorFactory() {
			@Override
			public Actor create() {
				return new ActorWithRxStash("receiver") {
					protected ActorMessageMatcher matcher;

					@Override
					public void preStart() {
						rxStash = rxStash
							.filter(msg -> msg.valueAsInt() > 50)
							.map(msg -> ActorMessage.create(msg.valueAsInt(), msg.tag()+1976, msg.source(), msg.dest()));
						
						matcher = new ActorMessageMatcher();
						matcher
							.match(0, msg -> stash.offer(msg))
							.match(msg -> msg.tag()>0, msg -> {
								rxStash.subscribe(System.out::println);
							});
					}
					
					@Override
					public void receive(ActorMessage<?> message) {
						if (!matcher.apply(message))
							unhandled(message);
					}
				}; 
			}
		});
		
		UUID sender = system.addActor(new ActorFactory() {
			@Override
			public Actor create() {
				return new Actor("Sender") {
					protected Random random;
					
					@Override
					public void preStart() {
						random = new Random();
					}
					
					@Override
					public void receive(ActorMessage<?> message) {
						send(ActorMessage.create(random.nextInt(100), random.nextInt(1+1), self(), receiver));
					}
				};
			}
		});
		
		system.start();
		
		system
			.timer().schedule(ActorMessage.create(null, 0, null, null), sender, 0, 100, TimeUnit.MILLISECONDS);
		
		try {
			Thread.sleep(15000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		system.shutdownWithActors(true);
	}

	public static void main(String[] args) {
		new ExampleRxStash();
	}
}
