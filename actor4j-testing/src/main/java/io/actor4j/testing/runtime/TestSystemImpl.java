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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
	
	public TestSystemImpl() {
		this(null);
	}

	public TestSystemImpl(TestSystemConfig config) {
		super(config);
		
		messageDispatcher = new TestActorMessageDispatcher(this);
		
		createPseudoActor(() -> new PseudoActor(this, true) {
			@Override
			public void receive(ActorMessage<?> message) {
				// empty, messages are only fetched via awaitMessage
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

	@Override
	public ActorCell underlyingCell(ActorId id) {
		return (ActorCell)id;
	}
	
	@Override
	public Actor underlyingActor(ActorId id) {
		InternalActorCell cell = (InternalActorCell)id;
		return (cell!=null)? cell.getActor() : null;
	}
	
	protected void testActor(Actor actor, List<Throwable> failures) {
		if (actor!=null && actor instanceof ActorTest) {
			testActorId = actor.getId();
			try {
				List<Story> list = ((ActorTest)actor).test();
				if (list!=null)
					for (Story story : list) {
						pseudoActor.reset();
						try {
							story.run();
						}
						catch (AssertionError | RuntimeException e) {
							failures.add(e);
						}
					}
			}
			finally {
				testActorId = null;
			}
		}
	}
	
	protected void throwIfFailed(List<Throwable> failures) {
		if (failures.isEmpty())
			return;
		Throwable first = failures.get(0);
		for (int i=1; i<failures.size(); i++)
			first.addSuppressed(failures.get(i));
		if (first instanceof AssertionError e)
			throw e;
		throw (RuntimeException)first;
	}

	
	@Override
	public void testActor(ActorId id) {
		List<Throwable> failures = new ArrayList<>();
		testActor(underlyingActor(id), failures);
		throwIfFailed(failures);
	}
	
	@Override
	public void testAllActors() {
		List<Throwable> failures = new ArrayList<>();
		internal_iterateCell((InternalActorCell)USER_ID, (cell) -> {
			testActor(cell.getActor(), failures);
			return false;
		});
		throwIfFailed(failures);
	}

	@Override
	public ActorMessage<?> awaitMessage(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
		return pseudoActor.await(timeout, unit);
	}
	
	@Override
	public void assertNoMessages() {
		ActorMessage<?> message = pseudoActor.peek();
		assertNull("Unexpected message: "+message, message);
	}
}
