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
package io.actor4j.examples.coap;

import io.actor4j.core.ActorService;
import io.actor4j.core.actors.Actor;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.examples.shared.ExamplesSettings;
import io.actor4j.web.coap.server.COAPActorService;

public class ExampleServerCoap {
	public static void main(String[] args) {
		ActorService actorService = ActorService.create(ExamplesSettings.factory());
		ActorId coap = actorService.addActor(() -> new Actor() {
			@Override
			public void receive(ActorMessage<?> message) {
				System.out.println(message);
			}
		});
		actorService.setAlias(coap, "coap");
		actorService.start();
		
		COAPActorService coapService = new COAPActorService() {
			@Override
			public ActorService getService() {
				return actorService;
			}
		};
		coapService.start();
	}
}
