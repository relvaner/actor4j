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
import java.util.function.Function;

import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;

// analysis of a state machine (single actor or overall system), results are computed on demand and kept
public class StateMachineAnalysis {
	protected final Graph<String, ActorVerificationEdge> graph;
	protected final String start;
	protected final Function<String, ActorVerificationSM> actors;

	protected List<List<String>> cycles;
	protected Set<String> unreachables;
	protected Set<String> dead;

	public StateMachineAnalysis(Graph<String, ActorVerificationEdge> graph, String start, Function<String, ActorVerificationSM> actors) {
		super();
		this.graph = graph;
		this.start = start;
		this.actors = actors;
	}

	public Graph<String, ActorVerificationEdge> graph() {
		return graph;
	}

	public String start() {
		return start;
	}

	// start at the initial state of the actor
	public StateMachineAnalysis from(String actor) {
		ActorVerificationSM sm = actors.apply(actor);
		if (sm.getInitialState()==null)
			throw new IllegalStateException("actor "+actor+" has no initial state");

		return new StateMachineAnalysis(graph, sm.getInitialState(), actors);
	}

	// start at a state of the actor
	public StateMachineAnalysis from(String actor, String state) {
		return new StateMachineAnalysis(graph, actors.apply(actor).getPrefix()+state, actors);
	}

	public List<List<String>> cycles() {
		if (cycles==null)
			cycles = ActorVerificationUtils.findCycles(graph);
		return cycles;
	}

	public Set<String> unreachables() {
		if (unreachables==null) {
			if (start==null)
				throw new IllegalStateException("no start state: use from(actor) or from(actor, state)");
			if (!graph.containsVertex(start))
				throw new IllegalStateException("start state "+start+" is not part of the state machine");
			unreachables = ActorVerificationUtils.findUnreachables(graph, start);
		}
		return unreachables;
	}

	// states without outgoing transitions, only reachable ones when a start state is known
	public Set<String> dead() {
		if (dead==null)
			dead = ActorVerificationUtils.findDead(graph, start!=null ? unreachables() : Set.of());
		return dead;
	}

	// states of cycles the actor can never leave again (no transition out of the cycle),
	// only reachable ones when a start state is known; dead() only covers states without any transition
	public Set<String> traps() {
		Set<String> result = new HashSet<>();
		for (Set<String> component : new KosarajuStrongConnectivityInspector<>(graph).stronglyConnectedSets()) {
			boolean cycle = component.size()>1 || graph.containsEdge(component.iterator().next(), component.iterator().next());
			boolean leaves = false;
			for (String state : component)
				for (ActorVerificationEdge edge : graph.outgoingEdgesOf(state))
					if (!component.contains(graph.getEdgeTarget(edge)))
						leaves = true;
			if (cycle && !leaves)
				result.addAll(component);
		}
		if (start!=null)
			result.removeAll(unreachables());

		return result;
	}

	@Override
	public String toString() {
		return "cycles="+cycles()+(start!=null ? ", unreachables="+unreachables() : "")+", dead="+dead();
	}
}
