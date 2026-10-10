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
package io.actor4j.polyglot.pods;

import java.util.HashMap;
import java.util.Map;

import io.actor4j.core.actors.ActorRef;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.pods.ActorPod;
import io.actor4j.core.pods.actors.PodActor;
import io.actor4j.core.pods.functions.PodFunction;
import io.actor4j.core.utils.Reply;
import io.actor4j.polyglot.api.ActorPolyglotAPI;
import io.actor4j.polyglot.api.ActorPolyglotMessage;
import io.actor4j.polyglot.pods.runtime.PolyglotContextImpl;
import io.actor4j.polyglot.pods.runtime.PolyglotContextInternal;
import io.actor4j.polyglot.query.PolyglotQueryRequest;
import io.actor4j.polyglot.state.PolyglotStateStore;
import io.actor4j.polyglot.streams.PolyglotStreams;

public abstract class PolyglotFunctionPod extends ActorPod {
	@Override
	public PodActor create() {
		return new PodActor() {
			protected PolyglotContextInternal contextPolyglot;
			protected PodFunction podFunction;
			protected Map<Long, ActorId> routes;
			
			@Override
			public void preStart() {
				if (isExposed())
					expose();
				
				if (getContext().isShard())
					setAlias(domain()+getContext().shardId());
				else
					setAlias(domain());
				
				routes = new HashMap<>();
				register();
				registerRoutes(routes);
				
				PolyglotFunctionPod.this.preStart(contextPolyglot);
			}
			
			@Override
			public void postStop() {
				try {
					PolyglotFunctionPod.this.postStop(contextPolyglot);
				}
				finally {
					contextPolyglot.close();
				}
			}
			
			@Override
			public void receive(ActorMessage<?> message) {
				contextPolyglot.api().router().put(ActorPolyglotMessage.SOURCE, message.source());
				contextPolyglot.api().router().put(ActorPolyglotMessage.DEST, message.dest());
				
				Reply result = podFunction.handle(filter(message));
				if (result!=null && result.tag()>=0)
					internal_callback(this, message, result);
//				else
//					NO_REPLY;
			}

			@Override
			public void register() {
				contextPolyglot = PolyglotContextImpl.create(languageId());
				
				ActorPolyglotAPI api = ActorPolyglotAPI.create(this, getContext());
				api.router().putAll(routes);
				contextPolyglot.injectAPI(api);
				
				PolyglotStateStore stateStore = createStateStore(contextPolyglot);
				if (stateStore!=null)
					contextPolyglot.injectStateStore(stateStore);
				PolyglotStreams streams = createStreams(contextPolyglot);
				if (streams!=null)
					contextPolyglot.injectStreams(streams);
				PolyglotQueryRequest queryRequest = createQueryRequest(contextPolyglot);
				if (queryRequest!=null)
					contextPolyglot.injectQueryRequest(queryRequest);
				
				podFunction = new PolyglotPodFunction(this, getContext(), contextPolyglot, script());
			}
		};
	}
	
	protected void internal_callback(ActorRef host, ActorMessage<?> message, Reply result) {
		host.tell(result.value(), result.tag(), message.source(), message.interaction(), message.protocol(), message.domain());
	}
	
	public PolyglotStateStore createStateStore(PolyglotContext contextPolyglot) {
		return null;
	}
	
	public PolyglotStreams createStreams(PolyglotContext contextPolyglot) {
		return null;
	}
	
	public PolyglotQueryRequest createQueryRequest(PolyglotContext contextPolyglot) {
		return null;
	}

	public abstract String languageId();
	public abstract CharSequence script();
	
	public ActorMessage<?> filter(ActorMessage<?> message) {
		return message;
	}
	
	public boolean isExposed() {
		return true;
	}
	
	public void registerRoutes(Map<Long, ActorId> routes) {
		// empty
	}
	
	public void preStart(PolyglotContext contextPolyglot) {
		// empty
	}
	
	public void postStop(PolyglotContext contextPolyglot) {
		// empty
	}
}
