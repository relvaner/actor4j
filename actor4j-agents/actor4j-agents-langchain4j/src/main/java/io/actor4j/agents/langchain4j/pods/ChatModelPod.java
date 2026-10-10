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

import io.actor4j.agents.pods.AgentFunctionPod;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.Reply;
import io.actor4j.functions.io.AsyncIORequest;
import io.actor4j.functions.io.AsyncIORequestHandler;
import io.actor4j.functions.pods.FunctionPodContext;
import io.actor4j.functions.pods.PodFunction;

public abstract class ChatModelPod extends AgentFunctionPod {
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
	public PodFunction createPodFunction(FunctionPodContext ctx) {
		return new PodFunction(ctx) {
			@Override
			public Reply handle(ActorMessage<?> message) {
				Reply result = null;
				
				if (resourceId.equals(message.source()))
					result = handleIoRequest(message);
				else {
					ctx.ioRequest().execute(message.value(), message.interaction());
					result = Reply.pending();
				}

				return result;
			}
			
			@Override
			public Reply handleIoRequest(ActorMessage<?> message) {
				return ChatModelPod.this.handleIoRequest(message);
			}
		};
	}
	
	public abstract Reply handleIoRequest(ActorMessage<?> message);
}
