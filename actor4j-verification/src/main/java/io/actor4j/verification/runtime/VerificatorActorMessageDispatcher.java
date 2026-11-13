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
package io.actor4j.verification.runtime;

import io.actor4j.core.id.ActorId;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.runtime.ActorSystemImpl;
import io.actor4j.core.runtime.DefaultActorMessageDispatcher;

// records every message for the conformance check
public class VerificatorActorMessageDispatcher extends DefaultActorMessageDispatcher {
	public VerificatorActorMessageDispatcher(ActorSystemImpl system) {
		super(system);
	}

	@Override
	public void unsafe_post(ActorMessage<?> message, ActorId source, String alias) {
		record(message, alias);
		super.unsafe_post(message, source, alias);
	}

	@Override
	public void post(ActorMessage<?> message, ActorId source, String alias) {
		record(message, alias);
		super.post(message, source, alias);
	}

	@Override
	public void postOuter(ActorMessage<?> message) {
		record(message, null);
		super.postOuter(message);
	}

	@Override
	public void postServer(ActorMessage<?> message) {
		record(message, null);
		super.postServer(message);
	}

	@Override
	public void postPriority(ActorMessage<?> message) {
		record(message, null);
		super.postPriority(message);
	}

	protected void record(ActorMessage<?> message, String alias) {
		if (message!=null)
			((VerificatorActorSystemImpl)system).record(message, alias);
	}
}
