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

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import java.util.Map.Entry;

import io.actor4j.bdd.Story;
import io.actor4j.core.ActorCell;
import io.actor4j.core.ActorSystem;
import io.actor4j.core.DefaultActorSystemImpl;
import io.actor4j.core.actors.Actor;
import io.actor4j.core.actors.PseudoActor;
import io.actor4j.core.messages.ActorMessage;

import static org.junit.Assert.*;

public class TestSystemImpl extends DefaultActorSystemImpl  {
	protected PseudoActor pseudoActor;
	protected volatile UUID pseudoActorId;
	protected volatile UUID testActorId;
	protected volatile CompletableFuture<ActorMessage<?>> actualMessage;
	
	public TestSystemImpl(ActorSystem wrapper) {
		this(null, wrapper);
	}

	public TestSystemImpl(String name, ActorSystem wrapper) {
		super(name, wrapper);
		
		messageDispatcher = new TestActorMessageDispatcher(this);
	}
	
	public ActorCell underlyingCell(UUID id) {
		return getCells().get(id);
	}
	
	public Actor underlyingActor(UUID id) {
		ActorCell cell = getCells().get(id);
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
	
	public void testActor(UUID id) {
		testActor(underlyingActor(id));
	}
	
	public void testAllActors() {
		Iterator<Entry<UUID, ActorCell>> iterator = getCells().entrySet().iterator();
		while (iterator.hasNext())
			testActor(iterator.next().getValue().getActor());
	}

	public ActorMessage<?> awaitMessage(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
		return pseudoActor.await(timeout, unit);
	}
	
	public void assertNoMessages() {
		assertFalse(pseudoActor.runOnce());
	}
}
