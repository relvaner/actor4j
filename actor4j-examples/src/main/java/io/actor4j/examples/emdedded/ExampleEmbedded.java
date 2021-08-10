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
package io.actor4j.examples.emdedded;

import static io.actor4j.core.logging.ActorLogger.logger;

import java.util.UUID;

import io.actor4j.core.ActorSystem;
import io.actor4j.core.actors.EmbeddedActor;
import io.actor4j.core.actors.EmbeddedHostActor;
import io.actor4j.core.messages.ActorMessage;

public class ExampleEmbedded {
	protected final int SWAP=22;
	
	public ExampleEmbedded() {
		ActorSystem system = new ActorSystem();
		
		UUID host = system.addActor(() -> new EmbeddedHostActor("host") {
			protected EmbeddedActor client;
			@Override
			public void preStart() {
				client = new EmbeddedActor("host:client", this) {
					@Override
					public boolean receive(ActorMessage<?> message) {
						boolean result = false;
						
						if (message.tag == SWAP) {
							become(msg -> {
								logger().info(String.format(
										"Received String message: %s", msg.valueAsString()));
								unbecome(); 
								return true;
							}, false);
							result = true;
						}
						
						return result;
					}
				};
			}
			
			@Override
			public void receive(ActorMessage<?> message) {
				if (!client.embedded(message))
					unhandled(message);
			}
		});
		
		system.send(new ActorMessage<Object>(null, SWAP, system.SYSTEM_ID, host));
		system.send(new ActorMessage<Object>("Hello World!", 0, system.SYSTEM_ID, host));
		system.send(new ActorMessage<Object>(null, SWAP, system.SYSTEM_ID, host));
		system.send(new ActorMessage<Object>("Hello World Again!", 0, system.SYSTEM_ID, host));
		system.start();
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		system.shutdownWithActors(true);
	}
	
	public static void main(String[] args) {
		new ExampleEmbedded();
	}
}
