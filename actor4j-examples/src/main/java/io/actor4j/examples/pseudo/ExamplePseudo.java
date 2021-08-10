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
package io.actor4j.examples.pseudo;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import io.actor4j.core.ActorSystem;
import io.actor4j.core.actors.Actor;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorFactory;
import io.actor4j.corex.actors.PseudoActorWithRx;

public class ExamplePseudo {
	public ExamplePseudo() {
		ActorSystem system = new ActorSystem();
		
		PseudoActorWithRx main = new PseudoActorWithRx(system, false) {
			@Override
			public void receive(ActorMessage<?> message) {
			}
		};
		
		UUID numberGenerator = system.addActor(new ActorFactory() {
			@Override
			public Actor create() {
				return new Actor("numberGenerator") {
					protected ScheduledFuture<?> timerFuture;
					
					@Override
					public void preStart() {
						Random random = new Random();
						timerFuture = system.timer()
							.schedule(() -> new ActorMessage<Integer>(random.nextInt(512), 0, self(), null), main.getId(), 0, 100, TimeUnit.MILLISECONDS);
					}
					
					@Override
					public void receive(ActorMessage<?> message) {
						System.out.printf("numberGenerator received a message.tag (%d) from main%n", message.tag);
					}
					
					@Override
					public void postStop() {
						timerFuture.cancel(true);
					}
				};
			}
		});
		
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			protected int i;
			@Override
			public void run() {
				main.runWithRx()
					.take(2)
					.map(msg -> "-> main received a message.value ("+msg.valueAsInt()+") from numberGenerator")
					.subscribe(System.out::println);
					
				main.send(new ActorMessage<>(null, i++, main.getId(), numberGenerator));
			}
		}, 0, 1000);
		
		system.start();
		
		try {
			Thread.sleep(15000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		timer.cancel();
		system.shutdownWithActors(true);
	}
	
	public static void main(String[] args) {
		new ExamplePseudo();
	}
}
