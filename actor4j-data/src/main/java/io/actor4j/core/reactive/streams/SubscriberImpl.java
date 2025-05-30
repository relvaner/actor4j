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

import io.actor4j.core.actors.ActorRef;
import io.actor4j.core.function.Procedure;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.messages.ActorMessage;

import static io.actor4j.core.reactive.streams.ActorReactiveStreamsTag.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SubscriberImpl {
	protected ActorRef actorRef;
	
	protected Map<ActorId, Consumer<Object>> onNextMap;
	protected Map<ActorId, Consumer<String>> onErrorMap; 
	protected Map<ActorId, Procedure> onCompleteMap;
	
	public SubscriberImpl(ActorRef actorRef) {
		super();
		this.actorRef = actorRef;
		
		onNextMap = new HashMap<>();
		onErrorMap = new HashMap<>();
		onCompleteMap = new HashMap<>();
	}
	
	public void receive(ActorMessage<?> message) {
		if (message.source()!=null) {
			if (message.tag()==ON_NEXT) {
				Consumer<Object> onNext = onNextMap.get(message.source()); 
				if (onNext!=null)
					onNext.accept(message.value());
			}
			else if (message.tag()==ON_ERROR) {
				Consumer<String> onError = onErrorMap.get(message.source()); 
				if (onError!=null)
					onError.accept(message.valueAsString());
			}
			else if (message.tag()==ON_COMPLETE) {
				Procedure onComplete = onCompleteMap.get(message.source()); 
				if (onComplete!=null)
					onComplete.apply();
			}
		}
	}
	
	public void subscribe(ActorId dest, Consumer<Object> onNext, Consumer<String> onError, Procedure onComplete) {
		if (onNext!=null)
			onNextMap.put(dest, onNext);
		if (onError!=null)
			onErrorMap.put(dest, onError);
		if (onComplete!=null)
			onCompleteMap.put(dest, onComplete);
	}
	
	public void unsubscribe(ActorId dest) {
		cancel(dest);
		
		onNextMap.remove(dest);
		onErrorMap.remove(dest);
		onCompleteMap.remove(dest);
	}
	
	public void request(long n, ActorId dest) {
		actorRef.tell(n, SUBSCRIPTION_REQUEST, dest);
	}
	
	public void requestReset(long n, ActorId dest) {
		actorRef.tell(n, SUBSCRIPTION_REQUEST_RESET, dest);
	}
	
	protected void cancel(ActorId dest) {
		actorRef.tell(null, SUBSCRIPTION_CANCEL, dest);
	}
	
	public void bulk(ActorId dest) {
		actorRef.tell(null, SUBSCRIPTION_BULK, dest);
	}
	
	public void cancelBulk(ActorId dest) {
		actorRef.tell(null, SUBSCRIPTION_CANCEL_BULK, dest);
	}
}
