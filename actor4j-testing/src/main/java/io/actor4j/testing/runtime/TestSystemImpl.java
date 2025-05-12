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
package io.actor4j.testing.runtime;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;

import io.actor4j.bdd.Story;
import io.actor4j.core.ActorCell;
import io.actor4j.core.actors.Actor;
import io.actor4j.core.actors.PseudoActor;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.runtime.DefaultActorSystemImpl;
import io.actor4j.core.runtime.InternalActorCell;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.testing.ActorTest;
import io.actor4j.testing.TestSystem;
import io.actor4j.testing.config.TestSystemConfig;

import static org.junit.Assert.*;

public class TestSystemImpl extends DefaultActorSystemImpl implements TestSystem {
	protected PseudoActor pseudoActor;
	protected volatile ActorId pseudoActorId;
	protected volatile ActorId testActorId;
	protected volatile CompletableFuture<ActorMessage<?>> actualMessage;
	
	public TestSystemImpl() {
		this(null);
	}

	public TestSystemImpl(TestSystemConfig config) {
		super(config);
		
		messageDispatcher = new TestActorMessageDispatcher(this);
		
		createPseudoActor(() -> new PseudoActor(this, true) {
			@Override
			public void receive(ActorMessage<?> message) {
				getActualMessage().complete(message);
			}
		});
	}
	
	@Override
	public boolean setConfig(TestSystemConfig config) {
		return super.setConfig(config);
	}
	
	public void createPseudoActor(Supplier<PseudoActor> factory) {
		pseudoActor = factory.get();
		pseudoActorId = pseudoActor.getId();
	}
	
	public CompletableFuture<ActorMessage<?>> getActualMessage() {
		return actualMessage;
	}

	@Override
	public ActorCell underlyingCell(ActorId id) {
		return (ActorCell)id;
	}
	
	@Override
	public Actor underlyingActor(ActorId id) {
		InternalActorCell cell = (InternalActorCell)id;
		return (cell!=null)? cell.getActor() : null;
	}
	
	protected void testActor(Actor actor) {
		if (actor!=null && actor instanceof ActorTest) {
			testActorId = actor.getId();
			List<Story> list = ((ActorTest)actor).test();
			if (list!=null)
				for (Story story : list) {
					pseudoActor.reset();
					try { // workaround, Java hangs, when an AssertionError is thrown!
						story.run();
					}
					catch (AssertionError e) {
						e.printStackTrace();
					}
				}
			testActorId = null;
		}
	}
	
	@Override
	public void testActor(ActorId id) {
		testActor(underlyingActor(id));
	}
	
	@Override
	public void testAllActors() {
		Function<InternalActorCell, Boolean> testAll = (cell) -> {
			testActor(cell.getActor());
			return false;
		};
		internal_iterateCell((InternalActorCell)USER_ID, testAll);
	}

	@Override
	public ActorMessage<?> awaitMessage(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
		return pseudoActor.await(timeout, unit);
	}
	
	@Override
	public void assertNoMessages() {
		assertFalse(pseudoActor.runOnce());
	}
}
