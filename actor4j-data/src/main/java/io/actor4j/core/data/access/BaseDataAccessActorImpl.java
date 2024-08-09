/*
 * Copyright (c) 2015-2024, David A. Bauer. All rights reserved.
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
package io.actor4j.core.data.access;

import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.CircuitBreaker;

import static io.actor4j.core.data.access.DataAccessActor.*;
import static io.actor4j.core.actors.ActorWithCache.*;

import io.actor4j.core.actors.Actor;
import io.actor4j.core.actors.ActorRef;

public abstract class BaseDataAccessActorImpl<K, V> {
	protected final ActorRef dataAccess;
	
	protected final CircuitBreaker circuitBreaker;
	
	public BaseDataAccessActorImpl(ActorRef dataAccess, int maxFailures, long resetTimeout) {
		super();
		this.dataAccess = dataAccess;
		
		circuitBreaker = new CircuitBreaker(maxFailures, resetTimeout);;
	}
	
	public BaseDataAccessActorImpl(ActorRef dataAccess) {
		this(dataAccess, DEFAULT_MAX_FAILURES, DEFAULT_RESET_TIMEOUT);
	}
	
	public abstract void onReceiveMessage(ActorMessage<?> msg, PersistentDataAccessDTO<K,V> dto);
	
	public abstract boolean hasOne(ActorMessage<?> msg, PersistentDataAccessDTO<K,V> dto);
	
	public abstract void findOne(ActorMessage<?> msg, PersistentDataAccessDTO<K,V> dto);
	public abstract void findAll(ActorMessage<?> msg, PersistentDataAccessDTO<K,V> dto);
	public abstract void insertOne(ActorMessage<?> msg, PersistentDataAccessDTO<K,V> dto);
	public abstract void replaceOne(ActorMessage<?> msg, PersistentDataAccessDTO<K,V> dto);
	public abstract void updateOne(ActorMessage<?> msg, PersistentDataAccessDTO<K,V> dto);
	public abstract void deleteOne(ActorMessage<?> msg, PersistentDataAccessDTO<K,V> dto);
	public abstract void queryOne(ActorMessage<?> msg, PersistentDataAccessDTO<K,V> dto);
	public abstract void queryAll(ActorMessage<?> msg, PersistentDataAccessDTO<K,V> dto);
	
	public abstract boolean handleMessage(ActorMessage<?> msg, PersistentDataAccessDTO<K,V> dto);
	
	public abstract void onSuccess(ActorMessage<?> msg, PersistentDataAccessDTO<K,V> dto);
	public abstract void onFailure(ActorMessage<?> msg, PersistentDataAccessDTO<K,V> dto, Throwable t);

	public void receive(ActorMessage<?> message) {
		if (message.value()!=null && message.value() instanceof PersistentDataAccessDTO) {
			@SuppressWarnings("unchecked")
			PersistentDataAccessDTO<K,V> dto = (PersistentDataAccessDTO<K,V>)message.value();
			
			if (circuitBreaker.isCallable()) {
				try {
					onReceiveMessage(message, dto);
					
					boolean unhandled = false;
					if (message.tag()==HAS_ONE) {
						if (hasOne(message, dto))
							dataAccess.tell(dto.shallowCopy(true), HAS_ONE, message.source(), message.interaction());
						else
							dataAccess.tell(dto.keyExists() ? dto.shallowCopy(false) : dto, HAS_ONE, message.source(), message.interaction());
					}
					else if (message.tag()==FIND_ONE || message.tag()==GET)
						findOne(message, dto);
					else if (message.tag()==FIND_ALL || message.tag()==GET_ALL)
						findAll(message, dto);
					else if (message.tag()==SET) {
						if (!((boolean)dto.reserved()) && !hasOne(message, dto))
							insertOne(message, dto);
						else
							replaceOne(message, dto);
					}
					else if (message.tag()==INSERT_ONE)
						insertOne(message, dto);
					else if (message.tag()==REPLACE_ONE)
						replaceOne(message, dto);
					else if (message.tag()==UPDATE_ONE || message.tag()==UPDATE)
						updateOne(message, dto);
					else if (message.tag()==DELETE_ONE)
						deleteOne(message, dto);
					else if (message.tag()==QUERY_ONE)
						queryOne(message, dto);
					else if (message.tag()==QUERY_ALL)
						queryAll(message, dto);
					else if (handleMessage(message, dto))
						;
					else {
						unhandled = true;
						((Actor)dataAccess).unhandled(message);
					}
					
					if (!unhandled) {
						circuitBreaker.success();
						onSuccess(message, dto);
					}
					else
						dataAccess.tell(dto, ActorMessage.UNHANDLED, message.source(), message.interaction());
				}
				catch(Exception e) { 
					e.printStackTrace();
					
					circuitBreaker.failure();
					onFailure(message, dto, e);
				}
			}

//			systemLogger().log(DEBUG, "circuit breaker: "+circuitBreaker.getState());
		}
		else
			((Actor)dataAccess).unhandled(message);
	}
}
