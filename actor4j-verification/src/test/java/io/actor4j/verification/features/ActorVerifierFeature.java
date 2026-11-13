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

import java.util.List;
import java.util.Set;

import org.junit.Test;

import io.actor4j.verification.*;

// ActorVerifier, VerificationResult, StateMachineAnalysis and VerificationAssert with the ping-pong example
public class ActorVerifierFeature {
	protected VerificationResult pingPong() {
		return ActorVerifier.verify(new VerifiableActor("ping", VerificationTestUtils::ping), new VerifiableActor("pong", VerificationTestUtils::pong));
	}

	@Test
	public void test_single_actors() {
		VerificationResult result = pingPong();

		assertEquals(Set.of(Set.of("ping:PING"), Set.of("ping:A", "ping:B", "ping:C")), normalize(result.actor("ping").cycles()));
		assertEquals(Set.of("ping:A", "ping:B", "ping:C"), result.actor("ping").unreachables());
		assertEquals(Set.of("ping:D"), result.actor("ping").dead());
		assertTrue(result.actor("pong").unreachables().isEmpty());
	}

	@Test
	public void test_overall_system_from_an_actor() {
		VerificationResult result = pingPong();

		assertEquals("ping:PING", result.all().from("ping").start());
		assertEquals(Set.of("ping:A", "ping:B", "ping:C"), result.all().from("ping").unreachables());
		assertEquals(Set.of("ping:D"), result.all().from("ping").dead());
		assertEquals("ping:B", result.all().from("ping", "B").start());
	}

	@Test(expected=IllegalStateException.class)
	public void test_overall_system_needs_a_start_for_unreachables() {
		pingPong().all().unreachables();
	}

	@Test
	public void test_assertions() {
		VerificationResult result = pingPong();

		VerificationAssert.assertCycle(result.all(), "ping:PING", "pong:PONG");
		VerificationAssert.assertNoUnreachableStates(result.actor("pong"));
		VerificationAssert.assertNoDeadStates(result.actor("pong"));
		try {
			VerificationAssert.assertNoDeadStates(result.all().from("ping"));
			fail();
		}
		catch (AssertionError e) {
			assertEquals("dead states: [ping:D]", e.getMessage());
		}
	}

	@Test
	public void test_same_names_by_unique_name() {
		VerificationResult result = ActorVerifier.verifyModels(List.of(
			sm("worker").initial("S").on(1).from("S").to("T"),
			sm("worker").initial("S").on(3).from("S").to("T")));

		assertEquals("worker#1:S", result.actor("worker#1").start());
		assertEquals("worker#2:S", result.actor("worker#2").start());
		try {
			result.actor("worker");
			fail();
		}
		catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("worker#1"));
		}
	}

	@Test
	public void test_report() {
		String report = pingPong().report();

		assertTrue(report.contains("ping: cycles="));
		assertTrue(report.contains("pong: cycles="));
		assertTrue(report.contains("all (from ping:PING):"));
		assertTrue(report.contains("dead=[ping:D]"));
	}
}
