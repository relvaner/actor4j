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
package io.actor4j.agents.langchain4j.pods;

import java.util.UUID;

import io.actor4j.agents.pods.AgentRemoteFunctionPod;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.pods.RemotePodMessage;
import io.actor4j.core.utils.Reply;
import io.actor4j.functions.io.AsyncIORequest;
import io.actor4j.functions.io.AsyncIORequestHandler;
import io.actor4j.functions.pods.FunctionPodContext;
import io.actor4j.functions.pods.PodRemoteFunction;

public abstract class ChatModelRemotePod extends AgentRemoteFunctionPod {
	protected ActorId resourceId;
	
	public abstract ChatModelResourceActor createChatModelResourceActor();
	
	@Override
	public void preStart(FunctionPodContext ctx) {
		resourceId = ctx.host().getSystem().addActor(() -> createChatModelResourceActor());
	}
	
	@Override
	public AsyncIORequest createIORequest(FunctionPodContext ctx) {
		return new AsyncIORequest(new AsyncIORequestHandler() {
			@Override
			public void execute(Object value, UUID interaction) {
				ctx.host().tell(value, 0, resourceId, interaction);
			}
		});
	}
	
	@Override
	public PodRemoteFunction createPodRemoteFunction(FunctionPodContext ctx) {
		return new PodRemoteFunction(ctx) {
			@Override
			public Reply handle(ActorMessage<?> message, UUID interaction) {
				Reply result = null;
				
				if (resourceId.equals(message.source()))
					result = handleIoRequest(message, interaction);
				else {
					ctx.ioRequest().execute(message.value(), interaction);
					result = Reply.pending();
				}

				return result;
			}
			
			@Override
			public Reply handle(RemotePodMessage remoteMessage, UUID interaction) {
				ctx.ioRequest().execute(remoteMessage.remotePodMessageDTO().payload(), interaction);
				return Reply.pending();
			}
			
			@Override
			public Reply handleIoRequest(ActorMessage<?> message, UUID interaction) {
				return ChatModelRemotePod.this.handleIoRequest(message, interaction);
			}
		};
	}
	
	public abstract Reply handleIoRequest(ActorMessage<?> message, UUID interaction);
}
