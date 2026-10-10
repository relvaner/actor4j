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

import java.util.UUID;

import io.actor4j.core.actors.ActorRef;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.pods.ActorPod;
import io.actor4j.core.pods.RemotePodMessage;
import io.actor4j.core.pods.actors.PodActor;
import io.actor4j.core.pods.utils.PodStatus;
import io.actor4j.core.runtime.InternalActorSystem;
import io.actor4j.core.runtime.config.InternalServerCallback;
import io.actor4j.core.utils.Reply;
import io.actor4j.functions.query.AsyncQueryRequest;
import io.actor4j.functions.state.StateStore;
import io.actor4j.functions.streams.AsyncStreams;

public abstract class RemoteFunctionPod extends ActorPod {
	@Override
	public PodActor create() {
		return new PodActor() {
			protected FunctionPodContext contextFunction;
			protected PodRemoteFunction podRemoteFunction;

			@Override
			public void preStart() {
				if (isExposed())
					expose();
				
				if (getContext().isShard())
					setAlias(domain()+getContext().shardId());
				else
					setAlias(domain());

				register();
				
				RemoteFunctionPod.this.preStart(contextFunction);
			}
			
			@Override
			public void postStop() {
				RemoteFunctionPod.this.postStop(contextFunction);
			}

			@Override
			public void receive(ActorMessage<?> message) {
				if (message.value() instanceof RemotePodMessage remoteMessage) {
					UUID interaction = message.interaction()!=null ? message.interaction() : UUID.randomUUID();
					
					Reply result = podRemoteFunction.handle(filterRemote(remoteMessage), interaction);
					if (remoteMessage.remotePodMessageDTO().reply()) {
						if (result!=null && result.tag()>=0)
							internal_callback(this, remoteMessage, result);
						else
							internal_callback(this, remoteMessage, handleRejectedReply(result));
					}
				}
				else {
					Reply result = podRemoteFunction.handle(filter(message));
					if (result!=null && result.tag()>=0)
						internal_callback(this, message, result);
				}
			}
			
			@Override
			public void register() {
				contextFunction = FunctionPodContext.create(this, getContext());
				
				StateStore<?, ?> stateStore = createStateStore(contextFunction);
				if (stateStore!=null)
					contextFunction.injectStateStore(stateStore);
				AsyncStreams streams = createStreams(contextFunction);
				if (streams!=null)
					contextFunction.injectStreams(streams);
				AsyncQueryRequest queryRequest = createQueryRequest(contextFunction);
				if (queryRequest!=null)
					contextFunction.injectQueryRequest(queryRequest);
				
				podRemoteFunction = createPodRemoteFunction(contextFunction);
			}
		};
	}
	
	protected void internal_callback(ActorRef host, ActorMessage<?> message, Reply result) {
		host.tell(result.value(), result.tag(), message.source(), message.interaction(), message.protocol(), message.domain());
	}
	
	protected void internal_callback(ActorRef host, RemotePodMessage remoteMessage, Reply result) {
		InternalServerCallback internalServerCallback = ((InternalActorSystem)host.getSystem()).getRuntimeConfig().internalServerCallback();
		
		if (remoteMessage.remotePodMessageDTO().reply() && internalServerCallback!=null)
			internalServerCallback.accept(remoteMessage.replyAddress(), result.value(), result.tag());
	}
	
	public abstract PodRemoteFunction createPodRemoteFunction(FunctionPodContext contextFunction);
	
	public StateStore<?, ?> createStateStore(FunctionPodContext contextFunctions) {
		return null;
	}
	
	public AsyncStreams createStreams(FunctionPodContext contextFunctions) {
		return null;
	}
	
	public AsyncQueryRequest createQueryRequest(FunctionPodContext contextFunctions) {
		return null;
	}
	
	public ActorMessage<?> filter(ActorMessage<?> message) {
		return message;
	}
	
	public RemotePodMessage filterRemote(RemotePodMessage remoteMessage) {
		return remoteMessage;
	}
	
	public Reply handleRejectedReply(Reply result) {
		return Reply.of(result.value(), PodStatus.CONFLICT);
	}
	
	public boolean isExposed() {
		return true;
	}
	
	public void preStart(FunctionPodContext contextFunction) {
		// empty
	}
	
	public void postStop(FunctionPodContext contextFunction) {
		// empty
	}
}
