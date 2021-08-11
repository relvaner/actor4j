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
package io.actor4j.testing;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.actor4j.core.ActorSystem;
import io.actor4j.core.actors.Actor;
import io.actor4j.core.actors.PseudoActor;
import io.actor4j.core.config.ActorSystemConfig;
import io.actor4j.core.internal.ActorCell;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.testing.config.TestSystemConfig;
import io.actor4j.testing.internal.TestSystemImpl;

public class TestSystem extends ActorSystem {
	public TestSystem() {
		super((wrapper, c) -> new TestSystemImpl(wrapper, c), TestSystemConfig.create());
		
		((TestSystemImpl)system).createPseudoActor(() -> new PseudoActor(this, true) {
			@Override
			public void receive(ActorMessage<?> message) {
				((TestSystemImpl)system).getActualMessage().complete(message);
			}
		});
	}
	
	@Deprecated
	@Override
	public boolean setConfig(ActorSystemConfig config) {
		return false;
	}
	
	public boolean setConfig(TestSystemConfig config) {
		return super.setConfig(config);
	}
	
	public ActorCell underlyingCell(UUID id) {
		return ((TestSystemImpl)system).underlyingCell(id);
	}
	
	public Actor underlyingActor(UUID id) {
		return ((TestSystemImpl)system).underlyingActor(id);
	}
	
	public void testActor(UUID id) {
		((TestSystemImpl)system).testActor(id);
	}
	
	public void testAllActors() {
		((TestSystemImpl)system).testAllActors();
	}
	
	public ActorMessage<?> awaitMessage(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
		return ((TestSystemImpl)system).awaitMessage(timeout, unit);
	}
	
	public void assertNoMessages() {
		((TestSystemImpl)system).assertNoMessages();
	}
}
