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
package io.actor4j.examples.behaviour;

import static io.actor4j.core.logging.ActorLogger.*;

import io.actor4j.core.actors.Actor;
import io.actor4j.core.messages.ActorMessage;

public class MyActor extends Actor {
	protected final int SWAP=22;
	
	@Override
	public void receive(ActorMessage<?> message) {
		if (message.tag == SWAP)
			become(msg -> {
				logger().info(String.format(
						"Received String message: %s", msg.valueAsString()));
				unbecome();
			}, false);
		else
			unhandled(message);
	}
}
