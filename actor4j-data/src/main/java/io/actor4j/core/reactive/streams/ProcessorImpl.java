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

import io.actor4j.core.actors.ActorRef;
import io.actor4j.core.function.Procedure;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.messages.ActorMessage;

public class ProcessorImpl extends PublisherImpl {
	protected SubscriberImpl subscriberImpl;
	
	public ProcessorImpl(ActorRef actorRef) {
		super(actorRef);
		subscriberImpl = new SubscriberImpl(actorRef);
	}
	
	@Override
	public void receive(ActorMessage<?> message) {
		super.receive(message);
		subscriberImpl.receive(message);
	}
	
	public void subscribe(ActorId dest, Consumer<Object> onNext, Consumer<String> onError, Procedure onComplete) {
		subscriberImpl.subscribe(dest, onNext, onError, onComplete);
	}
	
	public void unsubscribe(ActorId dest) {
		subscriberImpl.unsubscribe(dest);
	}
	
	public void request(long n, ActorId dest) {
		subscriberImpl.request(n, dest);
	}
	
	public void requestReset(long n, ActorId dest) {
		subscriberImpl.requestReset(n, dest);
	}
	
	public void bulk(ActorId dest) {
		subscriberImpl.bulk(dest);
	}
	
	public void cancelBulk(ActorId dest) {
		subscriberImpl.cancelBulk(dest);
	}
}
