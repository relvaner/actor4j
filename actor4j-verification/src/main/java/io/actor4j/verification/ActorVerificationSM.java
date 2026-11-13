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
package io.actor4j.verification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;

import io.actor4j.core.actors.ActorRef;

public class ActorVerificationSM {
	protected Graph<String, ActorVerificationEdge> graph;
	protected ActorRef actor;
	protected String prefix;
	
	protected String initialState;
	// last transition (on/from ... to), target of send
	protected ActorVerificationEdge lastEdge;
	
	public ActorVerificationSM(ActorRef actor) {
		super();
		this.actor = actor;
		graph = new DefaultDirectedGraph<>(ActorVerificationEdge.class);
		prefix = (actor.getName()!=null ? actor.getName():actor.getId())+":"; 
	}
	
	public static ActorVerificationSM of(ActorRef actor) {
		return new ActorVerificationSM(actor);
	}

	public Graph<String, ActorVerificationEdge> getGraph() {
		return graph;
	}
	
	public String getName() {
		return actor.getName();
	}
	
	public String getPrefix() {
		return prefix;
	}
	
	// renames the prefix of all states, e.g. to distinguish actors with the same name (name#1, name#2)
	public ActorVerificationSM rename(String name) {
		String newPrefix = name+":";
		Graph<String, ActorVerificationEdge> renamed = new DefaultDirectedGraph<>(ActorVerificationEdge.class);
		for (String state : graph.vertexSet())
			renamed.addVertex(newPrefix+state.substring(prefix.length()));
		for (ActorVerificationEdge edge : graph.edgeSet())
			renamed.addEdge(
				newPrefix+graph.getEdgeSource(edge).substring(prefix.length()), 
				newPrefix+graph.getEdgeTarget(edge).substring(prefix.length()), 
				new ActorVerificationEdge(edge.events, edge.tuples));
		if (initialState!=null)
			initialState = newPrefix+initialState.substring(prefix.length());
		
		graph = renamed;
		prefix = newPrefix;
		
		return this;
	}
	
	public String getInitialState() {
		return initialState;
	}

	@Deprecated
	public String getIntialStateMarker() {
		return getInitialState();
	}

	public ActorVerificationSM addInitialStateMarker(String state) {
		initialState = prefix+state;
		graph.addVertex(initialState);
		
		return this;
	}

	public ActorVerificationSM addStateMarker(String state) {
		graph.addVertex(prefix+state);
		
		return this;
	}
	
	// states are created with the first transition (no addStateMarker needed)
	protected ActorVerificationEdge edge(String sourceState, String targetState) {
		graph.addVertex(prefix+sourceState);
		graph.addVertex(prefix+targetState);
		ActorVerificationEdge edge = graph.getEdge(prefix+sourceState, prefix+targetState);
		if (edge==null) {
			edge = new ActorVerificationEdge(null, null);
			graph.addEdge(prefix+sourceState, prefix+targetState, edge);
		}

		return edge;
	}

	protected ActorVerificationSM addTransition(String sourceState, String targetState, Set<Integer> events, List<ActorVerificationEdgeTuple> tuples) {
		graph.addVertex(prefix+sourceState);
		graph.addVertex(prefix+targetState);
		ActorVerificationEdge edge = graph.getEdge(prefix+sourceState, prefix+targetState);
		// copy and add: the collections of the caller stay untouched (may be immutable)
		if (edge!=null) {
			if (events!=null) {
				if (edge.events==null)
					edge.events = new HashSet<>();
				edge.events.addAll(events);
			}
			if (tuples!=null) {
				if (edge.tuples==null)
					edge.tuples = new ArrayList<>();
				edge.tuples.addAll(tuples);
			}
		}
		else
			graph.addEdge(prefix+sourceState, prefix+targetState, new ActorVerificationEdge(
				events!=null ? new HashSet<>(events) : null, tuples!=null ? new ArrayList<>(tuples) : null));
		
		return this;
	}
	
	protected ActorVerificationSM addTransition(String sourceState, String targetState, int event) {
		graph.addVertex(prefix+sourceState);
		graph.addVertex(prefix+targetState);
		ActorVerificationEdge edge = graph.getEdge(prefix+sourceState, prefix+targetState);
		if (edge!=null) {
			if (edge.events!=null)
				edge.events.add(event);
			else
				edge.events = new HashSet<Integer>(Arrays.asList(event));
		}
		else
			graph.addEdge(prefix+sourceState, prefix+targetState, new ActorVerificationEdge(new HashSet<Integer>(Arrays.asList(event)), null));
	
		return this;
	}
	
	protected ActorVerificationSM addTransition(String sourceState, String targetState, ActorVerificationEdgeTuple tuple) {
		graph.addVertex(prefix+sourceState);
		graph.addVertex(prefix+targetState);
		ActorVerificationEdge edge = graph.getEdge(prefix+sourceState, prefix+targetState);
		if (edge!=null) {
			if (edge.tuples!=null)
				edge.tuples.add(tuple);
			else
				edge.tuples = new ArrayList<>(List.of(tuple));
		}
		else
			graph.addEdge(prefix+sourceState, prefix+targetState, new ActorVerificationEdge(null, new ArrayList<>(List.of(tuple))));
	
		return this;
	}
	
	public ActorVerificationSM addInTransition(String sourceState, String targetState, Set<Integer> events) {
		return addTransition(sourceState, targetState, events, null);
	}
	
	public ActorVerificationSM addInTransition(String sourceState, String targetState, int event) {
		return addTransition(sourceState, targetState, event);
	}
	
	public ActorVerificationSM addOutTransition(String sourceState, String targetState, List<ActorVerificationEdgeTuple> tuples) {
		return addTransition(sourceState, targetState, null, tuples);
	}
	
	public ActorVerificationSM addOutTransition(String sourceState, String targetState, ActorVerificationEdgeTuple tuple) {
		return addTransition(sourceState, targetState, tuple);
	}
	
	public ActorVerificationSM addOutTransition(String sourceState, String targetState, int event, String... aliases) {
		return addTransition(sourceState, targetState, new ActorVerificationEdgeTuple(new HashSet<Integer>(Arrays.asList(event)), Arrays.asList(aliases)));
	}

	// builder: initial("S"), on(event).from("S").to("T").send(event, aliases), from("S").to("T").send(...)
	public ActorVerificationSM initial(String state) {
		return addInitialStateMarker(state);
	}

	public ActorVerificationSM state(String state) {
		return addStateMarker(state);
	}

	// transition triggered by one or more received events
	public Transition on(int... events) {
		Set<Integer> set = new HashSet<>();
		for (int event : events)
			set.add(event);

		return new Transition(set);
	}

	// transition without received event (e.g. only sending)
	public Transition.Source from(String sourceState) {
		return new Transition(null).from(sourceState);
	}

	// sends with the last transition (on/from ... to)
	public ActorVerificationSM send(int event, String... aliases) {
		if (lastEdge==null)
			throw new IllegalStateException("send without transition: use on(event).from(state).to(state) or from(state).to(state) first");
		if (lastEdge.tuples==null)
			lastEdge.tuples = new ArrayList<>();
		lastEdge.tuples.add(new ActorVerificationEdgeTuple(new HashSet<>(List.of(event)), new ArrayList<>(Arrays.asList(aliases))));

		return this;
	}

	public class Transition {
		protected final Set<Integer> events;

		protected Transition(Set<Integer> events) {
			this.events = events;
		}

		public Source from(String sourceState) {
			return new Source(sourceState);
		}

		public class Source {
			protected final String sourceState;

			protected Source(String sourceState) {
				this.sourceState = sourceState;
			}

			public ActorVerificationSM to(String targetState) {
				ActorVerificationEdge edge = edge(sourceState, targetState);
				if (events!=null) {
					if (edge.events==null)
						edge.events = new HashSet<>();
					edge.events.addAll(events);
				}
				lastEdge = edge;

				return ActorVerificationSM.this;
			}
		}
	}
}
