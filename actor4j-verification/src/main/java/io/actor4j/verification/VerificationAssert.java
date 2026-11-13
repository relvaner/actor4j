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
package io.actor4j.verification;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// assertions for tests, independent of the test framework (AssertionError)
public final class VerificationAssert {
	private VerificationAssert() {
	}

	public static void assertNoDeadStates(StateMachineAnalysis analysis) {
		if (!analysis.dead().isEmpty())
			throw new AssertionError("dead states: "+analysis.dead());
	}

	public static void assertNoUnreachableStates(StateMachineAnalysis analysis) {
		if (!analysis.unreachables().isEmpty())
			throw new AssertionError("unreachable states: "+analysis.unreachables());
	}

	public static void assertNoCycles(StateMachineAnalysis analysis) {
		if (!analysis.cycles().isEmpty())
			throw new AssertionError("cycles: "+analysis.cycles());
	}

	public static void assertConforms(ConformanceResult result) {
		if (!result.conforms())
			throw new AssertionError("not conform to the models: "+result.violations());
	}

	// a cycle over exactly these states exists (order and start do not matter)
	public static void assertCycle(StateMachineAnalysis analysis, String... states) {
		Set<String> expected = Set.of(states);
		for (List<String> cycle : analysis.cycles())
			if (new HashSet<>(cycle).equals(expected))
				return;
		throw new AssertionError("no cycle over "+expected+", cycles: "+analysis.cycles());
	}
}
