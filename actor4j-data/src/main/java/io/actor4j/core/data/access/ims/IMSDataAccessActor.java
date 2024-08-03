/*
 * Copyright (c) 2015-2024, David A. Bauer. All rights reserved.
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
package io.actor4j.core.data.access.ims;

import io.actor4j.core.actors.Actor;
import io.actor4j.core.messages.ActorMessage;

public class IMSDataAccessActor<K, V> extends Actor {
	protected IMSDataAccessActorImpl<K, V> impl;
	
	public IMSDataAccessActor(String name) {
		super(name);

		impl = new IMSDataAccessActorImpl<K, V>(this);
	}

	@Override
	public void receive(ActorMessage<?> message) {
		impl.receive(message);
	}
}
