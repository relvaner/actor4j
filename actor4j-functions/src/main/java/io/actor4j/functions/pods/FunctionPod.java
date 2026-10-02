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
package io.actor4j.functions.pods;

import io.actor4j.core.actors.ActorRef;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.pods.ActorPod;
import io.actor4j.core.pods.PodContext;
import io.actor4j.core.pods.actors.PodActor;
import io.actor4j.core.pods.functions.PodFunction;
import io.actor4j.core.utils.Pair;
import io.actor4j.functions.query.QueryRequest;
import io.actor4j.functions.state.StateStore;
import io.actor4j.functions.streams.Streams;

public abstract class FunctionPod extends ActorPod {
	@Override
	public PodActor create() {
		return new PodActor() {
			protected FunctionPodContext contextFunction;
			protected PodFunction podFunction;
			
			@Override
			public void preStart() {
				if (isExposed())
					expose();
				
				if (getContext().isShard())
					setAlias(domain()+getContext().shardId());
				else
					setAlias(domain());
				
				register();
				
				FunctionPod.this.preStart(this, getContext());
			}
			
			@Override
			public void postStop() {
				FunctionPod.this.postStop(this, getContext());
			}

			@Override
			public void receive(ActorMessage<?> message) {
				Pair<Object, Integer> result = podFunction.handle(filter(message));
				if (result!=null && result.b()>=0)
					internal_callback(this, message, result);
//				else
//					NO_REPLY;
			}
			
			@Override
			public void register() {
				contextFunction = FunctionPodContext.create();
				
				StateStore stateStore = createStateStore();
				if (stateStore!=null)
					contextFunction.injectStateStore(stateStore);
				Streams streams = createStreams(contextFunction);
				if (streams!=null)
					contextFunction.injectStreams(streams);
				QueryRequest queryRequest = createQueryRequest();
				if (queryRequest!=null)
					contextFunction.injectQueryRequest(queryRequest);
				
				podFunction = createPodFunction(this, getContext());
			}
		};
	}
	
	protected void internal_callback(ActorRef host, ActorMessage<?> message, Pair<Object, Integer> result) {
		host.tell(result.a(), result.b(), message.source(), message.interaction(), message.protocol(), message.domain());
	}
	
	public abstract PodFunction createPodFunction(ActorRef host, PodContext context);
	
	public StateStore createStateStore() {
		return null;
	}
	
	public Streams createStreams(FunctionPodContext contextFunctions) {
		return null;
	}
	
	public QueryRequest createQueryRequest() {
		return null;
	}
	
	public ActorMessage<?> filter(ActorMessage<?> message) {
		return message;
	}
	
	public boolean isExposed() {
		return true;
	}
	
	public void preStart(ActorRef host, PodContext context) {
		// empty
	}
	
	public void postStop(ActorRef host, PodContext context) {
		// empty
	}
}
