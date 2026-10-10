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
import java.util.function.Function;

import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.pods.RemotePodMessage;
import io.actor4j.core.utils.Reply;

public abstract class PodRemoteFunction extends PodFunction {
	public PodRemoteFunction(FunctionPodContext context, Function<Object, Object> defaultMapper) {
		super(context, defaultMapper);
	}
	
	public PodRemoteFunction(FunctionPodContext context) {
		super(context);
	}
	
	public abstract Reply handle(RemotePodMessage remoteMessage, UUID interaction);
	
	public Reply handleQueryRequest(ActorMessage<?> message) {
		return Reply.none();
	}
	
	public Reply handleIoRequest(ActorMessage<?> message) {
		return Reply.none();
	}
}
