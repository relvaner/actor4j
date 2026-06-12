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
package io.actor4j.polyglot.pod;

import io.actor4j.core.actors.ActorRef;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.pods.ActorPod;
import io.actor4j.core.pods.actors.PodActor;
import io.actor4j.core.pods.functions.PodFunction;
import io.actor4j.core.utils.Pair;

public abstract class PolyglotFunctionPod extends ActorPod {
	@Override
	public PodActor create() {
		return new PodActor() {
			protected PolyglotContext contextPolyglot;
			protected PodFunction podFunction;
			
			@Override
			public void preStart() {
				expose();
				
				if (getContext().isShard())
					setAlias(domain()+getContext().shardId());
				else
					setAlias(domain());
				
				register();
			}
			
			@Override
			public void postStop() {
				contextPolyglot.close();
			}
			
			@Override
			public void receive(ActorMessage<?> message) {
				Pair<Object, Integer> result = podFunction.handle(filter(message));
				if (result!=null)
					internal_callback(this, message, result);
			}

			@Override
			public void register() {
				contextPolyglot = PolyglotContext.create(languageId());
				podFunction = new PolyglotPodFuction(this, getContext(), contextPolyglot, script());
			}
		};
	}
	
	protected void internal_callback(ActorRef host, ActorMessage<?> message, Pair<Object, Integer> result) {
		host.tell(result.a(), result.b(), message.source(), message.interaction(), message.protocol(), message.domain());
	}

	public abstract String languageId();
	public abstract CharSequence script();
	
	public ActorMessage<?> filter(ActorMessage<?> message) {
		return message;
	}
}
