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
package io.actor4j.core.reactive.streams;

import java.util.function.Consumer;

import io.actor4j.core.actors.Actor;
import io.actor4j.core.function.Procedure;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.messages.ActorMessage;

public class ProcessorActor extends Actor {
	protected ProcessorImpl processorImpl;
	
	public ProcessorActor() {
		this(null);
	}
	
	public ProcessorActor(String name) {
		super(name);
		processorImpl = new ProcessorImpl(this);
	}
	
	@Override
	public void receive(ActorMessage<?> message) {
		processorImpl.receive(message);
	}
	
	public <T> void broadcast(T value) {
		processorImpl.broadcast(value);
	}
	
	public boolean isBulk(ActorId dest) {
		return processorImpl.isBulk(dest);
	}
	
	public <T> boolean onNext(T value, ActorId dest) {
		return processorImpl.onNext(value, dest);
	}
	
	public void onError(String error, ActorId dest) {
		processorImpl.onError(error, dest);
	}
	
	public void onComplete(ActorId dest) {
		processorImpl.onComplete(dest);
	}
	
	public void subscribe(ActorId dest, Consumer<Object> onNext, Consumer<String> onError, Procedure onComplete) {
		processorImpl.subscribe(dest, onNext, onError, onComplete);
	}
	
	public void unsubscribe(ActorId dest) {
		processorImpl.unsubscribe(dest);
	}
	
	public void request(long n, ActorId dest) {
		processorImpl.request(n, dest);
	}
	
	public void requestReset(long n, ActorId dest) {
		processorImpl.requestReset(n, dest);
	}
	
	public void bulk(ActorId dest) {
		processorImpl.bulk(dest);
	}
	
	public void cancelBulk(ActorId dest) {
		processorImpl.cancelBulk(dest);
	}
}
