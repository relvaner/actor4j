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

import io.actor4j.core.actors.Actor;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.messages.ActorMessage;

public class PublisherActor extends Actor {
	protected PublisherImpl publisherImpl;
	
	public PublisherActor() {
		this(null);
	}
	
	public PublisherActor(String name) {
		super(name);
		publisherImpl = new PublisherImpl(this);
	}
	
	@Override
	public void receive(ActorMessage<?> message) {
		publisherImpl.receive(message);
	}
	
	public <T> void broadcast(T value) {
		publisherImpl.broadcast(value);
	}
	
	public boolean isBulk(ActorId dest) {
		return publisherImpl.isBulk(dest);
	}
	
	public <T> boolean onNext(T value, ActorId dest) {
		return publisherImpl.onNext(value, dest);
	}
	
	public void onError(String error, ActorId dest) {
		publisherImpl.onError(error, dest);
	}
	
	public void onComplete(ActorId dest) {
		publisherImpl.onComplete(dest);
	}
}
