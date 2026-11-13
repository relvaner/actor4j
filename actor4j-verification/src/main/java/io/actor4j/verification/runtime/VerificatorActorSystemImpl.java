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
package io.actor4j.verification.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultDirectedGraph;

import io.actor4j.core.runtime.DefaultActorSystemImpl;
import io.actor4j.core.runtime.InternalActorCell;
import io.actor4j.verification.ActorVerification;
import io.actor4j.verification.ActorVerificationEdge;
import io.actor4j.verification.ActorVerificationSM;
import io.actor4j.verification.ActorVerificationUtils;
import io.actor4j.verification.ActorVerificator;
import io.actor4j.verification.config.ActorVerificationConfig;

public class VerificatorActorSystemImpl extends DefaultActorSystemImpl implements ActorVerificator {
	public VerificatorActorSystemImpl() {
		this(null);
	}

	public VerificatorActorSystemImpl(ActorVerificationConfig config) {
		super(config);
	}
	
	@Override
	public boolean setConfig(ActorVerificationConfig config) {
		return super.setConfig(config);
	}
	
	@Override
	public void verify(Consumer<ActorVerificationSM> consumer) {
		if (consumer!=null) {
			Function<InternalActorCell, Boolean> verifyAction = (cell) -> {
				if (cell.getActor() instanceof ActorVerification)
					consumer.accept(((ActorVerification)cell.getActor()).verify());
				return false;
			};
			internal_iterateCell((InternalActorCell)USER_ID, verifyAction);
		}				
	}
	
	@Override
	public void verifyAll(Consumer<ActorVerificationSM> consumer, Consumer<Graph<String, ActorVerificationEdge>> consumerAll) {
		List<ActorVerificationSM> list = new ArrayList<>();
		verify(list::add);
		
		// actors with the same name get unique prefixes (name#1, name#2, ...), otherwise their states would merge
		Map<String, Integer> count = new HashMap<>();
		for (ActorVerificationSM sm : list)
			count.merge(sm.getPrefix(), 1, Integer::sum);
		Map<String, Integer> index = new HashMap<>();
		for (ActorVerificationSM sm : list) {
			String prefix = sm.getPrefix();
			if (count.get(prefix)>1)
				sm.rename(prefix.substring(0, prefix.length()-1)+"#"+index.merge(prefix, 1, Integer::sum));
		}
		
		// overall graph with own edge objects: interconnect must not change the models of the single actors
		Graph<String, ActorVerificationEdge> graph = new DefaultDirectedGraph<>(ActorVerificationEdge.class);
		for (ActorVerificationSM sm : list) {
			if (consumer!=null)
				consumer.accept(sm);
			Graphs.addAllVertices(graph, sm.getGraph().vertexSet());
			for (ActorVerificationEdge edge : sm.getGraph().edgeSet())
				graph.addEdge(sm.getGraph().getEdgeSource(edge), sm.getGraph().getEdgeTarget(edge), new ActorVerificationEdge(
					edge.getEvents()!=null ? new HashSet<>(edge.getEvents()) : null, 
					edge.getTuples()!=null ? new ArrayList<>(edge.getTuples()) : null));
		}
		ActorVerificationUtils.interconnect(list, graph);
		
		if (consumerAll!=null)
			consumerAll.accept(graph);
	}
}
