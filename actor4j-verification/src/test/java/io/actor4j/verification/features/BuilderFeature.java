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

import java.util.Set;

import org.junit.Test;

import io.actor4j.verification.*;

// builder: initial, on(...).from(...).to(...).send(...), from(...).to(...)
public class BuilderFeature {
	@Test
	public void test_builder_creates_the_same_model_as_the_old_api() {
		ActorVerificationSM sm = sm("ping")
			.initial("PING")
			.on(PONG).from("PING").to("PING").send(PING, "pong")
			.on(0).from("PING").to("D")
			.on(0).from("A").to("B")
			.on(0).from("B").to("C")
			.on(0).from("C").to("A");
		ActorVerificationSM old = ping();

		assertEquals(old.getInitialState(), sm.getInitialState());
		assertEquals(old.getGraph().vertexSet(), sm.getGraph().vertexSet());
		for (ActorVerificationEdge edge : old.getGraph().edgeSet()) {
			ActorVerificationEdge other = sm.getGraph().getEdge(old.getGraph().getEdgeSource(edge), old.getGraph().getEdgeTarget(edge));
			assertNotNull(other);
			assertEquals(edge.getEvents(), other.getEvents());
			assertEquals(String.valueOf(edge.getTuples()), String.valueOf(other.getTuples()));
		}
	}

	@Test
	public void test_states_are_created_with_the_transition() {
		ActorVerificationSM sm = sm("a")
			.initial("S")
			.on(1).from("S").to("T");

		assertEquals(Set.of("a:S", "a:T"), sm.getGraph().vertexSet());
		assertEquals(Set.of(1), sm.getGraph().getEdge("a:S", "a:T").getEvents());
	}

	@Test
	public void test_old_api_creates_states_too() {
		// N1: no addStateMarker needed
		ActorVerificationSM sm = sm("a")
			.addInitialStateMarker("S")
			.addInTransition("S", "T", 1)
			.addOutTransition("T", "U", 2, "b");

		assertEquals(Set.of("a:S", "a:T", "a:U"), sm.getGraph().vertexSet());
	}

	@Test
	public void test_several_events_and_sends() {
		ActorVerificationSM sm = sm("a")
			.initial("S")
			.on(1, 2).from("S").to("T").send(3, "b").send(4, "c", "d");
		ActorVerificationEdge edge = sm.getGraph().getEdge("a:S", "a:T");

		assertEquals(Set.of(1, 2), edge.getEvents());
		assertEquals(2, edge.getTuples().size());
		assertEquals(Set.of(4), edge.getTuples().get(1).getEvents());
		assertEquals(java.util.List.of("c", "d"), edge.getTuples().get(1).getAliases());
	}

	@Test
	public void test_send_only_transition() {
		ActorVerificationSM sm = sm("a")
			.initial("IDLE")
			.from("IDLE").to("WAITING").send(1, "server");
		ActorVerificationEdge edge = sm.getGraph().getEdge("a:IDLE", "a:WAITING");

		assertNull(edge.getEvents());
		assertEquals(Set.of(1), edge.getTuples().get(0).getEvents());
	}

	@Test(expected=IllegalStateException.class)
	public void test_send_without_transition() {
		sm("a").initial("S").send(1, "b");
	}

	@Test
	public void test_builder_with_interconnect() {
		// receive and send on one transition, as in H2, written with the builder
		VerificationResult result = ActorVerifier.verifyModels(java.util.List.of(
			sm("a").initial("T").from("T").to("T").send(1, "b"),
			sm("b").initial("P").on(1).from("P").to("Q").send(2, "c"),
			sm("c").initial("R").on(2).from("R").to("R")));

		assertEquals(Set.of(1), result.all().graph().getEdge("a:T", "b:P").getEvents());
		assertEquals(Set.of(2), result.all().graph().getEdge("b:Q", "c:R").getEvents());
	}
}
