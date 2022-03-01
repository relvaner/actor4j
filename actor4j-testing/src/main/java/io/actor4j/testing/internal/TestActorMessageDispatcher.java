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
package io.actor4j.testing.internal;

import java.util.UUID;
import java.util.function.BiConsumer;

import io.actor4j.core.internal.ActorSystemImpl;
import io.actor4j.core.internal.ActorThread;
import io.actor4j.core.internal.DefaultActorMessageDispatcher;
import io.actor4j.core.messages.ActorMessage;

public class TestActorMessageDispatcher extends DefaultActorMessageDispatcher {

	public TestActorMessageDispatcher(ActorSystemImpl system) {
		super(system);
	}
	
	@Override
	public void post(ActorMessage<?> message, UUID source, String alias) {
		super.post(redirect(message), source, alias);
	}
	
	@Override
	protected void postQueue(ActorMessage<?> message, BiConsumer<ActorThread, ActorMessage<?>> biconsumer) {
		super.postQueue(redirect(message), biconsumer);
	}
	
	@Override
	public void postOuter(ActorMessage<?> message) {
		super.postOuter(redirect(message));
	}
	
	protected ActorMessage<?> redirect(ActorMessage<?> message) {
		if (message!=null && ((TestSystemImpl)system).testActorId!=null && message.dest()!=((TestSystemImpl)system).testActorId)
			return message.shallowCopy(((TestSystemImpl)system).pseudoActorId);
		else
			return message;
	}
}
