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
package io.actor4j.patterns.utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import io.actor4j.core.ActorSystem;
import io.actor4j.core.actors.ActorRef;
import io.actor4j.core.id.ActorId;
import io.actor4j.patterns.actors.FutureActor;
import io.actor4j.patterns.messages.FutureActorMessage;

public final class FuturePattern {
	public static <T> Future<T> ask(T value, int tag, ActorId dest, ActorRef actorRef) {
		ActorId mediator = actorRef.getSystem().addActor(() -> new FutureActor(dest, true));
		
		return askWithMediator(value, tag, mediator, actorRef);
	}
	
	public static <T> Future<T> askWithMediator(T value, int tag, ActorId mediator, ActorRef actorRef) {	
		CompletableFuture<T> result = new CompletableFuture<>();
		actorRef.send(new FutureActorMessage<T>(result, value, tag, actorRef.self(), mediator));
		
		return result;
	}
	
	public static <T> Future<T> ask(T value, int tag, ActorId dest, ActorSystem system) {
		ActorId mediator = system.addActor(() -> new FutureActor(dest, true));
		
		return askWithMediator(value, tag, mediator, system);
	}
	
	public static <T> Future<T> askWithMediator(T value, int tag, ActorId mediator, ActorSystem system) {	
		CompletableFuture<T> result = new CompletableFuture<>();
		system.send(new FutureActorMessage<T>(result, value, tag, system.SYSTEM_ID(), mediator));
		
		return result;
	}
}
