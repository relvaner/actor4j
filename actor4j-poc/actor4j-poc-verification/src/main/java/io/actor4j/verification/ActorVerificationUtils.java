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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.alg.cycle.JohnsonSimpleCycles;

public class ActorVerificationUtils {
	// find cycles
	public static <V, E> List<List<V>> findCycles(Graph<V, E> graph) {
		return new JohnsonSimpleCycles<>(graph).findSimpleCycles();
	}
	
	// find unreachable vertexes from the start vertex
	public static <V, E> Set<V> findUnreachables(Graph<V, E> graph, V startVertex) {  
		Set<V> result = new HashSet<>();
		 
		for (V endVertex : graph.vertexSet())
			if (endVertex!=startVertex && DijkstraShortestPath.findPathBetween(graph, startVertex, endVertex)==null)
				result.add(endVertex);
		 
		return result;
	} 
	
	// find vertexes that do not have outgoing connections
	public static <V, E> Set<V> findDead(Graph<V, E> graph, Set<V> unreachables) {
		Set<V> result = new HashSet<>(graph.vertexSet());
		result.removeAll(unreachables);
		
		Iterator<V> iterator = result.iterator();
		while (iterator.hasNext()) 
			if (graph.outDegreeOf(iterator.next())>0)
				iterator.remove();
		
		return result;
	}
	
	public static void interconnect(List<ActorVerificationSM> list, Graph<String, ActorVerificationEdge> graph) {
		Set<Integer> outEvents = new HashSet<>();
		Set<Integer> outEventsCopy = new HashSet<>();
		
		for (ActorVerificationSM sm : list)
			for (ActorVerificationEdge outEdge : sm.getGraph().edgeSet())
				if (outEdge.tuples!=null) {
					outEvents.clear();
					for (ActorVerficationEdgeTuple tuple : outEdge.tuples)
						outEvents.addAll(tuple.events);
					
					//System.out.println(outEvents);
					
					for (ActorVerificationSM _sm : list) {
						for (ActorVerificationEdge inEdge : _sm.getGraph().edgeSet())
							if (inEdge.tuples==null) {
								//System.out.println(inEdge);
								outEventsCopy.clear();
								outEventsCopy.addAll(outEvents);
								outEventsCopy.retainAll(inEdge.events);
								if (!outEventsCopy.isEmpty()) {
									for (Integer event : outEvents) {
										ActorVerificationEdge newEdge = graph.getEdge(outEdge.getTarget(), inEdge.getSource());
										if (newEdge!=null)
											newEdge.events.add(event);
										else
											graph.addEdge(outEdge.getTarget(), inEdge.getSource(), new ActorVerificationEdge(new HashSet<>(), null));
									}
								}
							}
						// self reference edges
						for (String vertex : _sm.getGraph().vertexSet()) {
							ActorVerificationEdge inEdge = _sm.getGraph().getEdge(vertex, vertex);
							if (inEdge!=null) {
								//System.out.println(inEdge);
								outEventsCopy.clear();
								outEventsCopy.addAll(outEvents);
								outEventsCopy.retainAll(inEdge.events);
								if (!outEventsCopy.isEmpty()) {
									for (Integer event : outEvents) {
										ActorVerificationEdge newEdge = graph.getEdge(outEdge.getTarget(), inEdge.getSource());
										if (newEdge!=null)
											newEdge.events.add(event);
										else
											graph.addEdge(outEdge.getTarget(), inEdge.getSource(), new ActorVerificationEdge(new HashSet<>(), null));
									}
								}
							}
						}
					}
				}
			
	}
}
