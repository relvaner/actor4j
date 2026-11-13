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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.junit.Test;

import io.actor4j.verification.*;

// regression tests for interconnect (review 2026-10: H1, H2, M2)
public class InterconnectFeature {
	protected Graph<String, ActorVerificationEdge> interconnect(ActorVerificationSM... sms) {
		List<ActorVerificationSM> list = List.of(sms);
		Graph<String, ActorVerificationEdge> graph = new DefaultDirectedGraph<>(ActorVerificationEdge.class);
		for (ActorVerificationSM sm : list)
			Graphs.addGraph(graph, sm.getGraph());
		ActorVerificationUtils.interconnect(list, graph);
		return graph;
	}

	@Test
	public void test_connection_carries_its_event() {
		Graph<String, ActorVerificationEdge> graph = interconnect(ping(), pong());

		assertEquals(Set.of(PING), graph.getEdge("ping:PING", "pong:PONG").getEvents());
		assertEquals(Set.of(PONG), graph.getEdge("pong:PONG", "ping:PING").getEvents());
	}

	@Test
	public void test_only_addressed_actors_are_connected() {
		// H1: other accepts PING too, but ping sends PING only to pong
		ActorVerificationSM other = sm("other")
			.addInitialStateMarker("O")
			.addInTransition("O", "O", PING);
		Graph<String, ActorVerificationEdge> graph = interconnect(ping(), pong(), other);

		assertNotNull(graph.getEdge("ping:PING", "pong:PONG"));
		assertNull(graph.getEdge("ping:PING", "other:O"));
	}

	@Test
	public void test_receive_and_send_on_the_same_transition() {
		// H2: b receives 1 in P, sends 2 to c and changes to Q
		ActorVerificationSM a = sm("a")
			.addInitialStateMarker("T")
			.addOutTransition("T", "T", 1, "b");
		ActorVerificationSM b = sm("b")
			.addInitialStateMarker("P")
			.addStateMarker("Q")
			.addInTransition("P", "Q", 1)
			.addOutTransition("P", "Q", 2, "c");
		ActorVerificationSM c = sm("c")
			.addInitialStateMarker("R")
			.addInTransition("R", "R", 2);
		Graph<String, ActorVerificationEdge> graph = interconnect(a, b, c);

		assertEquals(Set.of(1), graph.getEdge("a:T", "b:P").getEvents());
		assertEquals(Set.of(2), graph.getEdge("b:Q", "c:R").getEvents());
	}

	@Test
	public void test_self_message_onto_send_only_transition() {
		// M2: the connection U -> T already exists as send-only transition (events==null), no NPE
		ActorVerificationSM x = sm("x")
			.addInitialStateMarker("T")
			.addStateMarker("U")
			.addInTransition("T", "T", 1)
			.addOutTransition("T", "U", 1, "x")
			.addOutTransition("U", "T", 2, "y");
		Graph<String, ActorVerificationEdge> graph = interconnect(x);

		assertEquals(Set.of(1), graph.getEdge("x:U", "x:T").getEvents());
	}

	@Test
	public void test_only_matching_events_are_connected() {
		// a sends 1 and 2 to b on one transition, b only accepts 2
		ActorVerificationSM a = sm("a")
			.addInitialStateMarker("T")
			.addOutTransition("T", "T", new ActorVerificationEdgeTuple(new HashSet<>(Set.of(1, 2)), List.of("b")));
		ActorVerificationSM b = sm("b")
			.addInitialStateMarker("P")
			.addInTransition("P", "P", 2);
		Graph<String, ActorVerificationEdge> graph = interconnect(a, b);

		assertEquals(Set.of(2), graph.getEdge("a:T", "b:P").getEvents());
	}
}
