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
package io.actor4j.verification.features;

import static io.actor4j.verification.features.VerificationTestUtils.*;
import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.jgrapht.Graph;
import org.junit.Test;

import io.actor4j.verification.*;

// verifyAll of the ActorVerificator (review 2026-10: M3, M4)
public class VerificatorFeature {
	protected Graph<String, ActorVerificationEdge> verifyAll(List<ActorVerificationSM> sms, VerifiableActor... actors) {
		ActorVerificator verificator = ActorVerificator.create();
		for (VerifiableActor actor : actors)
			verificator.addActor(() -> actor);

		AtomicReference<Graph<String, ActorVerificationEdge>> result = new AtomicReference<>();
		verificator.verifyAll(sms::add, result::set);
		return result.get();
	}

	@Test
	public void test_verifyAll_interconnects_the_actors() {
		// M4: no own call of interconnect needed
		Graph<String, ActorVerificationEdge> graph = verifyAll(new LinkedList<>(),
			new VerifiableActor("ping", VerificationTestUtils::ping), new VerifiableActor("pong", VerificationTestUtils::pong));

		assertEquals(Set.of(PING), graph.getEdge("ping:PING", "pong:PONG").getEvents());
		assertEquals(Set.of(PONG), graph.getEdge("pong:PONG", "ping:PING").getEvents());
	}

	@Test
	public void test_verifyAll_keeps_the_models_of_the_actors() {
		// interconnect works on own edge objects of the overall graph
		List<ActorVerificationSM> sms = new LinkedList<>();
		verifyAll(sms, new VerifiableActor("ping", VerificationTestUtils::ping), new VerifiableActor("pong", VerificationTestUtils::pong));

		for (ActorVerificationSM sm : sms)
			assertEquals(sm.getName().equals("ping") ? 5 : 1, sm.getGraph().edgeSet().size());
	}

	@Test
	public void test_actors_with_the_same_name_stay_separate() {
		// M3: two workers with the same name, each with its own transition
		Graph<String, ActorVerificationEdge> graph = verifyAll(new LinkedList<>(),
			new VerifiableActor("worker", () -> sm("worker").addInitialStateMarker("S").addStateMarker("T").addInTransition("S", "T", 1)),
			new VerifiableActor("worker", () -> sm("worker").addInitialStateMarker("S").addStateMarker("T").addInTransition("S", "T", 3)));

		assertEquals(Set.of("worker#1:S", "worker#1:T", "worker#2:S", "worker#2:T"), graph.vertexSet());
		Set<Set<Integer>> events = Set.of(
			graph.getEdge("worker#1:S", "worker#1:T").getEvents(), 
			graph.getEdge("worker#2:S", "worker#2:T").getEvents());
		assertEquals(Set.of(Set.of(1), Set.of(3)), events);
	}

	@Test
	public void test_actors_with_the_same_name_are_addressed_by_alias() {
		// a sends 1 to "worker": both workers are connected
		Graph<String, ActorVerificationEdge> graph = verifyAll(new LinkedList<>(),
			new VerifiableActor("a", () -> sm("a").addInitialStateMarker("T").addOutTransition("T", "T", 1, "worker")),
			new VerifiableActor("worker", () -> sm("worker").addInitialStateMarker("S").addInTransition("S", "S", 1)),
			new VerifiableActor("worker", () -> sm("worker").addInitialStateMarker("S").addInTransition("S", "S", 1)));

		assertNotNull(graph.getEdge("a:T", "worker#1:S"));
		assertNotNull(graph.getEdge("a:T", "worker#2:S"));
	}

	@Test
	public void test_rename_keeps_states_and_initial_state() {
		ActorVerificationSM sm = ping().rename("ping#1");

		assertEquals("ping#1:PING", sm.getIntialStateMarker());
		assertTrue(sm.getGraph().containsEdge("ping#1:A", "ping#1:B"));
		assertEquals(Set.of(PONG), sm.getGraph().getEdge("ping#1:PING", "ping#1:PING").getEvents());
		assertEquals("ping", sm.getName());
	}
}
