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

import java.util.List;

import org.jgrapht.Graph;

// result of ActorVerifier: analysis of every actor and of the overall system
public class VerificationResult {
	protected final List<ActorVerificationSM> list;
	protected final Graph<String, ActorVerificationEdge> graph;

	public VerificationResult(List<ActorVerificationSM> list, Graph<String, ActorVerificationEdge> graph) {
		super();
		this.list = list;
		this.graph = graph;
	}

	public List<ActorVerificationSM> models() {
		return list;
	}

	// state machine by name, for actors with the same name by unique name (name#1, name#2, ...)
	public ActorVerificationSM model(String name) {
		for (ActorVerificationSM sm : list)
			if (sm.getPrefix().equals(name+":"))
				return sm;

		ActorVerificationSM result = null;
		for (ActorVerificationSM sm : list)
			if (name.equals(sm.getName())) {
				if (result!=null)
					throw new IllegalArgumentException("more than one actor with the name "+name+": use "+name+"#1, "+name+"#2, ...");
				result = sm;
			}
		if (result==null)
			throw new IllegalArgumentException("no actor with the name "+name);

		return result;
	}

	// single actor, starting at its initial state
	public StateMachineAnalysis actor(String name) {
		ActorVerificationSM sm = model(name);
		return new StateMachineAnalysis(sm.getGraph(), sm.getInitialState(), this::model);
	}

	// overall system (actors interconnected), without start state: use from(actor)
	public StateMachineAnalysis all() {
		return new StateMachineAnalysis(graph, null, this::model);
	}

	public String report() {
		StringBuilder builder = new StringBuilder();
		for (ActorVerificationSM sm : list) {
			String name = sm.getPrefix().substring(0, sm.getPrefix().length()-1);
			builder.append(name).append(": ").append(actor(name)).append('\n');
		}
		StateMachineAnalysis all = all();
		if (!list.isEmpty() && list.get(0).getInitialState()!=null)
			all = new StateMachineAnalysis(graph, list.get(0).getInitialState(), this::model);
		builder.append("all").append(all.start()!=null ? " (from "+all.start()+")" : "").append(": ").append(all);

		return builder.toString();
	}

	@Override
	public String toString() {
		return report();
	}
}
