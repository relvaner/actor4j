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

import static io.actor4j.core.reactive.streams.ActorReactiveStreamsTag.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.actor4j.core.actors.ActorRef;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorGroup;
import io.actor4j.core.utils.ActorGroupSet;

public class PublisherImpl {
	protected ActorRef actorRef;
	
	protected ActorGroup subscribers;
	protected Iterator<ActorId> iteratorSubscribers;
	protected Map<ActorId, Long> requests;
	protected Map<ActorId, Boolean> bulks;
	
	public PublisherImpl(ActorRef actorRef) {
		super();
		this.actorRef = actorRef;
		subscribers = new ActorGroupSet();
		requests = new HashMap<>();
		bulks = new HashMap<>();
	}
	
	public void receive(ActorMessage<?> message) {
		if (message.source()!=null) {
			if (message.tag()==SUBSCRIPTION_REQUEST || message.tag()==SUBSCRIPTION_REQUEST_RESET) { //Validation: Long -> OnError
				long request = message.valueAsLong();
				if (!subscribers.add(message.source()) && message.tag()==SUBSCRIPTION_REQUEST) {
					request += requests.get(message.source());
					if (Long.MAX_VALUE-request<0)
						request = Long.MAX_VALUE;
				}
			
				requests.put(message.source(), request);
			}
			else if (message.tag()==SUBSCRIPTION_CANCEL)
				cancel(message.source());
			else if (message.tag()==SUBSCRIPTION_BULK)
				bulks.put(message.source(), true);
			else if (message.tag()==SUBSCRIPTION_CANCEL_BULK)
				bulks.remove(message.source());
		}
	}
	
	public void cancel(ActorId dest) {
		if (iteratorSubscribers!=null)
			iteratorSubscribers.remove();
		else
			subscribers.remove(dest);
		requests.remove(dest);
		bulks.remove(dest);
	}
	
	public <T> void broadcast(T value) {
		iteratorSubscribers = subscribers.iterator();
		while(iteratorSubscribers.hasNext())
			onNext(value, iteratorSubscribers.next());
		iteratorSubscribers = null;
	}
	
	public boolean isBulk(ActorId dest) {
		return bulks.get(dest)!=null;
	}
	
	public <T> boolean onNext(T value, ActorId dest) {
		boolean result = false;
		
		if (dest!=null) {
			Long request = requests.get(dest);
			
			if (request!=null) {
				if (request==Long.MAX_VALUE)
					actorRef.tell(value, ON_NEXT, dest);
				else if (request>0) {
					requests.put(dest, request-1);
					actorRef.tell(value, ON_NEXT, dest);
				
					if (request==1)
						onComplete(dest);
				}
				
				result = true;
			}
		}
		
		return result;
	}
	
	public void onError(String error, ActorId dest) {
		actorRef.tell(error, ON_ERROR, dest);
		cancel(dest);
	}
	
	public void onComplete(ActorId dest) {
		actorRef.tell(null, ON_COMPLETE, dest);
		cancel(dest);
	}
}
