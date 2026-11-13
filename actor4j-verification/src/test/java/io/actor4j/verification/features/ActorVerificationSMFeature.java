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

// building the state machine (review 2026-10: H3, M1)
public class ActorVerificationSMFeature {
	@Test
	public void test_states_are_prefixed_with_the_actor_name() {
		ActorVerificationSM sm = sm("worker")
			.addInitialStateMarker("S")
			.addStateMarker("T")
			.addInTransition("S", "T", 1);

		assertEquals("worker:S", sm.getInitialState());
		assertEquals(Set.of("worker:S", "worker:T"), sm.getGraph().vertexSet());
		assertEquals(Set.of(1), sm.getGraph().getEdge("worker:S", "worker:T").getEvents());
	}

	@Test
	public void test_in_transitions_are_added() {
		ActorVerificationSM sm = sm("worker")
			.addInitialStateMarker("S")
			.addInTransition("S", "S", 1)
			.addInTransition("S", "S", 2);

		assertEquals(Set.of(1, 2), sm.getGraph().getEdge("worker:S", "worker:S").getEvents());
	}

	@Test
	public void test_two_out_transitions_on_the_same_edge() {
		ActorVerificationSM sm = sm("worker")
			.addInitialStateMarker("S")
			.addOutTransition("S", "S", 1, "a")
			.addOutTransition("S", "S", 2, "b");

		assertEquals(2, sm.getGraph().getEdge("worker:S", "worker:S").getTuples().size());
	}

	@Test
	public void test_in_transition_set_then_single_event() {
		ActorVerificationSM sm = sm("worker")
			.addInitialStateMarker("S")
			.addStateMarker("T")
			.addInTransition("S", "T", Set.of(1, 2))
			.addInTransition("S", "T", 3);

		assertEquals(Set.of(1, 2, 3), sm.getGraph().getEdge("worker:S", "worker:T").getEvents());
	}

	@Test
	public void test_in_transition_single_event_then_set() {
		ActorVerificationSM sm = sm("worker")
			.addInitialStateMarker("S")
			.addStateMarker("T")
			.addInTransition("S", "T", 1)
			.addInTransition("S", "T", Set.of(2));

		assertEquals(Set.of(1, 2), sm.getGraph().getEdge("worker:S", "worker:T").getEvents());
	}
}
