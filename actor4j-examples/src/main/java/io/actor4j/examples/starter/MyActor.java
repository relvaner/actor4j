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
package io.actor4j.examples.starter;

import static io.actor4j.core.logging.ActorLogger.*;

import io.actor4j.core.actors.Actor;
import io.actor4j.core.messages.ActorMessage;

public class MyActor extends Actor {
	@Override
	public void receive(ActorMessage<?> message) {
		if (message.value instanceof String) {
			logger().info(String.format(
					"Received String message: %s", message.valueAsString()));
			send(message, message.source);
		} 
		else
			unhandled(message);
	}
}
