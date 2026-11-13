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
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.junit.Test;

import io.actor4j.verification.*;

// analysis of single state machines and of the documentation example (ping-pong)
public class DocumentationFeature {
	@Test
	public void test_single_ping() {
		ActorVerificationSM sm = ping();
		Set<String> unreachables = ActorVerificationUtils.findUnreachables(sm.getGraph(), sm.getIntialStateMarker());

		assertEquals(Set.of(Set.of("ping:PING"), Set.of("ping:A", "ping:B", "ping:C")), normalize(ActorVerificationUtils.findCycles(sm.getGraph())));
		assertEquals(Set.of("ping:A", "ping:B", "ping:C"), unreachables);
		assertEquals(Set.of("ping:D"), ActorVerificationUtils.findDead(sm.getGraph(), unreachables));
	}

	@Test
	public void test_single_pong() {
		ActorVerificationSM sm = pong();
		Set<String> unreachables = ActorVerificationUtils.findUnreachables(sm.getGraph(), sm.getIntialStateMarker());

		assertEquals(Set.of(Set.of("pong:PONG")), normalize(ActorVerificationUtils.findCycles(sm.getGraph())));
		assertTrue(unreachables.isEmpty());
		assertTrue(ActorVerificationUtils.findDead(sm.getGraph(), unreachables).isEmpty());
	}

	@Test
	public void test_all_ping_pong() {
		List<ActorVerificationSM> list = List.of(ping(), pong());
		Graph<String, ActorVerificationEdge> graph = new DefaultDirectedGraph<>(ActorVerificationEdge.class);
		for (ActorVerificationSM sm : list)
			Graphs.addGraph(graph, sm.getGraph());
		ActorVerificationUtils.interconnect(list, graph);

		Set<String> unreachables = ActorVerificationUtils.findUnreachables(graph, "ping:PING");
		assertEquals(Set.of(
			Set.of("ping:PING"), Set.of("pong:PONG"), Set.of("ping:PING", "pong:PONG"), Set.of("ping:A", "ping:B", "ping:C")),
			normalize(ActorVerificationUtils.findCycles(graph)));
		assertEquals(Set.of("ping:A", "ping:B", "ping:C"), unreachables);
		assertEquals(Set.of("ping:D"), ActorVerificationUtils.findDead(graph, unreachables));
	}

	@Test
	public void test_verificator_collects_all_actors() {
		ActorVerificator verificator = ActorVerificator.create();
		verificator.addActor(() -> new VerifiableActor("ping", VerificationTestUtils::ping));
		verificator.addActor(() -> new VerifiableActor("pong", VerificationTestUtils::pong));

		List<String> names = new LinkedList<>();
		AtomicReference<Graph<String, ActorVerificationEdge>> all = new AtomicReference<>();
		verificator.verifyAll((sm) -> names.add(sm.getName()), all::set);

		assertEquals(Set.of("ping", "pong"), Set.copyOf(names));
		assertTrue(all.get().containsVertex("ping:PING"));
		assertTrue(all.get().containsVertex("pong:PONG"));
	}
}
