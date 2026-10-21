/*
 * Copyright (c) 2015-2026, David A. Bauer. All rights reserved.
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
package io.actor4j.testing.runtime;

import io.actor4j.core.runtime.ActorSystemImpl;
import io.actor4j.core.runtime.DefaultActorMessageDispatcher;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.messages.ActorMessage;

public class TestActorMessageDispatcher extends DefaultActorMessageDispatcher {

	public TestActorMessageDispatcher(ActorSystemImpl system) {
		super(system);
	}
	
	@Override
	public void unsafe_post(ActorMessage<?> message, ActorId source, String alias) {
		ActorMessage<?> redirected = redirect(message);
		if (redirected!=message)
			super.post(redirected, source, null); // pseudo actor has no actor thread, unsafe path would drop it
		else
			super.unsafe_post(message, source, alias);
	}
	
	@Override
	public void post(ActorMessage<?> message, ActorId source, String alias) {
		ActorMessage<?> redirected = redirect(message);
		super.post(redirected, source, redirected!=message ? null : alias);
	}
	
	@Override
	public void postOuter(ActorMessage<?> message) {
		super.postOuter(redirect(message));
	}
	
	@Override
	public void postServer(ActorMessage<?> message) {
		super.postServer(redirect(message));
	}
	
	@Override
	public void postPriority(ActorMessage<?> message) {
		super.postPriority(redirect(message));
	}

	protected ActorMessage<?> redirect(ActorMessage<?> message) {
		ActorId testActorId = ((TestSystemImpl)system).testActorId;
		if (message!=null && testActorId!=null && message.source()==testActorId && message.dest()!=testActorId)
			return message.shallowCopy(((TestSystemImpl)system).pseudoActorId);
		else
			return message;
	}
}
