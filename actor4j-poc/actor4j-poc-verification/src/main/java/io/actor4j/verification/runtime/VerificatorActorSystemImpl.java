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

import java.util.function.Consumer;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultDirectedGraph;

import io.actor4j.core.runtime.DefaultActorSystemImpl;
import io.actor4j.core.runtime.InternalActorCell;
import io.actor4j.verification.ActorVerification;
import io.actor4j.verification.ActorVerificationEdge;
import io.actor4j.verification.ActorVerificationSM;
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
		if (consumer!=null)
			for (InternalActorCell cell : cells.values())
				if (cell.getActor() instanceof ActorVerification)
					consumer.accept(((ActorVerification)cell.getActor()).verify());		
	}
	
	@Override
	public void verifyAll(Consumer<ActorVerificationSM> consumer, Consumer<Graph<String, ActorVerificationEdge>> consumerAll) {
		Graph<String, ActorVerificationEdge> graph = new DefaultDirectedGraph<>(ActorVerificationEdge.class);
		verify((sm) -> {
			if (consumer!=null)
				consumer.accept(sm);
			Graphs.addGraph(graph, sm.getGraph());
		});
		
		if (consumerAll!=null)
			consumerAll.accept(graph);
	}
}
