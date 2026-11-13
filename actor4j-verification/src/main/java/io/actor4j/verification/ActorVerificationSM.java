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
	
	protected String intialStateMarker;
	
	public ActorVerificationSM(ActorRef actor) {
		super();
		this.actor = actor;
		graph = new DefaultDirectedGraph<>(ActorVerificationEdge.class);
		prefix = (actor.getName()!=null ? actor.getName():actor.getId())+":"; 
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
		if (intialStateMarker!=null)
			intialStateMarker = newPrefix+intialStateMarker.substring(prefix.length());
		
		graph = renamed;
		prefix = newPrefix;
		
		return this;
	}
	
	public String getIntialStateMarker() {
		return intialStateMarker;
	}

	public ActorVerificationSM addInitialStateMarker(String state) {
		intialStateMarker = prefix+state;
		graph.addVertex(intialStateMarker);
		
		return this;
	}

	public ActorVerificationSM addStateMarker(String state) {
		graph.addVertex(prefix+state);
		
		return this;
	}
	
	protected ActorVerificationSM addTransition(String sourceState, String targetState, Set<Integer> events, List<ActorVerificationEdgeTuple> tuples) {
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
	};
	
	protected ActorVerificationSM addTransition(String sourceState, String targetState, int event) {
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
	};
	
	protected ActorVerificationSM addTransition(String sourceState, String targetState, ActorVerificationEdgeTuple tuple) {
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
	};
	
	public ActorVerificationSM addInTransition(String sourceState, String targetState, Set<Integer> events) {
		return addTransition(sourceState, targetState, events, null);
	};
	
	public ActorVerificationSM addInTransition(String sourceState, String targetState, int event) {
		return addTransition(sourceState, targetState, event);
	};
	
	public ActorVerificationSM addOutTransition(String sourceState, String targetState, List<ActorVerificationEdgeTuple> tuples) {
		return addTransition(sourceState, targetState, null, tuples);
	}
	
	public ActorVerificationSM addOutTransition(String sourceState, String targetState, ActorVerificationEdgeTuple tuple) {
		return addTransition(sourceState, targetState, tuple);
	}
	
	public ActorVerificationSM addOutTransition(String sourceState, String targetState, int event, String... aliases) {
		return addTransition(sourceState, targetState, new ActorVerificationEdgeTuple(new HashSet<Integer>(Arrays.asList(event)), Arrays.asList(aliases)));
	}
}
