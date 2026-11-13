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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.alg.cycle.JohnsonSimpleCycles;
import org.jgrapht.graph.DefaultDirectedGraph;

public class ActorVerificationUtils {
	// find cycles
	public static <V, E> List<List<V>> findCycles(Graph<V, E> graph) {
		// copy with plain (non-intrusive) edges: since jgrapht 1.5.1 an intrusive edge (DefaultEdge) may only belong
		// to one graph, but JohnsonSimpleCycles adds the edges of its input to its own subgraphs
		Graph<V, Object> copy = new DefaultDirectedGraph<>(null, Object::new, false);
		Graphs.addAllVertices(copy, graph.vertexSet());
		for (E edge : graph.edgeSet())
			copy.addEdge(graph.getEdgeSource(edge), graph.getEdgeTarget(edge));
		
		return new JohnsonSimpleCycles<>(copy).findSimpleCycles();
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
	
	// actors with the same name get unique prefixes (name#1, name#2, ...), otherwise their states would merge
	public static void uniqueNames(List<ActorVerificationSM> list) {
		Map<String, Integer> count = new HashMap<>();
		for (ActorVerificationSM sm : list)
			count.merge(sm.getPrefix(), 1, Integer::sum);
		Map<String, Integer> index = new HashMap<>();
		for (ActorVerificationSM sm : list) {
			String prefix = sm.getPrefix();
			if (count.get(prefix)>1)
				sm.rename(prefix.substring(0, prefix.length()-1)+"#"+index.merge(prefix, 1, Integer::sum));
		}
	}

	// overall graph with own edge objects (interconnect must not change the models of the single actors)
	public static Graph<String, ActorVerificationEdge> overallGraph(List<ActorVerificationSM> list) {
		Graph<String, ActorVerificationEdge> graph = new DefaultDirectedGraph<>(ActorVerificationEdge.class);
		for (ActorVerificationSM sm : list) {
			Graphs.addAllVertices(graph, sm.getGraph().vertexSet());
			for (ActorVerificationEdge edge : sm.getGraph().edgeSet())
				graph.addEdge(sm.getGraph().getEdgeSource(edge), sm.getGraph().getEdgeTarget(edge), new ActorVerificationEdge(
					edge.getEvents()!=null ? new HashSet<>(edge.getEvents()) : null,
					edge.getTuples()!=null ? new ArrayList<>(edge.getTuples()) : null));
		}
		interconnect(list, graph);

		return graph;
	}

	public static void interconnect(List<ActorVerificationSM> list, Graph<String, ActorVerificationEdge> graph) {
		for (ActorVerificationSM sender : list)
			for (ActorVerificationEdge outEdge : sender.getGraph().edgeSet())
				if (outEdge.tuples!=null)
					for (ActorVerificationEdgeTuple tuple : outEdge.tuples)
						for (ActorVerificationSM receiver : list)
							if (tuple.aliases.contains(receiver.getName()))
								for (ActorVerificationEdge inEdge : receiver.getGraph().edgeSet())
									if (inEdge.events!=null) {
										Set<Integer> matching = new HashSet<>(tuple.events);
										matching.retainAll(inEdge.events);
										if (!matching.isEmpty())
											connect(graph, outEdge.getTarget(), inEdge.getSource(), matching);
									}
	}

	protected static void connect(Graph<String, ActorVerificationEdge> graph, String source, String target, Set<Integer> events) {
		ActorVerificationEdge edge = graph.getEdge(source, target);
		if (edge==null)
			graph.addEdge(source, target, new ActorVerificationEdge(new HashSet<>(events), null));
		else if (edge.events==null)
			edge.events = new HashSet<>(events);
		else
			edge.events.addAll(events);
	}
}
