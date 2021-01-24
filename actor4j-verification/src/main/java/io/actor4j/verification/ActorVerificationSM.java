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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;

import io.actor4j.core.actors.Actor;

public class ActorVerificationSM {
	protected Graph<String, ActorVerificationEdge> graph;
	protected Actor actor;
	protected String prefix;
	
	protected String intialStateMarker;
	
	public ActorVerificationSM(Actor actor) {
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
	
	protected ActorVerificationSM addTransition(String sourceState, String targetState, Set<Integer> events, List<ActorVerficationEdgeTuple> tuples) {
		ActorVerificationEdge edge = graph.getEdge(prefix+sourceState, prefix+targetState);
		if (edge!=null) {
			if (events!=null)
				edge.events = events;
			if (tuples!=null)
				edge.tuples = tuples;
		}
		else
			graph.addEdge(prefix+sourceState, prefix+targetState, new ActorVerificationEdge(events, tuples));
		
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
	
	protected ActorVerificationSM addTransition(String sourceState, String targetState, ActorVerficationEdgeTuple tuple) {
		ActorVerificationEdge edge = graph.getEdge(prefix+sourceState, prefix+targetState);
		if (edge!=null) {
			if (edge.tuples!=null)
				edge.tuples.add(tuple);
			else
				edge.tuples = Arrays.asList(tuple);
		}
		else
			graph.addEdge(prefix+sourceState, prefix+targetState, new ActorVerificationEdge(null, Arrays.asList(tuple)));
	
		return this;
	};
	
	public ActorVerificationSM addInTransition(String sourceState, String targetState, Set<Integer> events) {
		return addTransition(sourceState, targetState, events, null);
	};
	
	public ActorVerificationSM addInTransition(String sourceState, String targetState, int event) {
		return addTransition(sourceState, targetState, event);
	};
	
	public ActorVerificationSM addOutTransition(String sourceState, String targetState, List<ActorVerficationEdgeTuple> tuples) {
		return addTransition(sourceState, targetState, null, tuples);
	}
	
	public ActorVerificationSM addOutTransition(String sourceState, String targetState, ActorVerficationEdgeTuple tuple) {
		return addTransition(sourceState, targetState, tuple);
	}
	
	public ActorVerificationSM addOutTransition(String sourceState, String targetState, int event, String... aliases) {
		return addTransition(sourceState, targetState, new ActorVerficationEdgeTuple(new HashSet<Integer>(Arrays.asList(event)), Arrays.asList(aliases)));
	}
}
