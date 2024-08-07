/*
 * Copyright (c) 2015-2018, David A. Bauer. All rights reserved.
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
package io.actor4j.core.data.access.utils;

import static io.actor4j.core.actors.ActorWithCache.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.actor4j.core.actors.ActorRef;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorFactory;
import io.actor4j.core.utils.ActorGroup;
import io.actor4j.core.utils.ActorGroupSet;
import io.actor4j.core.utils.Pair;
import io.actor4j.core.data.access.AckMode;
import io.actor4j.core.data.access.PrimaryVolatileCacheActor;
import io.actor4j.core.data.access.SecondaryVolatileCacheActor;
import io.actor4j.core.data.access.VolatileDTO;
import static io.actor4j.core.data.access.AckMode.*;

public class VolatileActorCacheManager<K, V> {
	protected ActorRef actorRef;
	protected String cacheAlias;
	protected AckMode ackMode;
	
	public VolatileActorCacheManager(ActorRef actorRef, String cacheAlias) {
		super();
		
		this.actorRef = actorRef;
		this.cacheAlias = cacheAlias;
	}
	
	public ActorFactory create(int instances, int cacheSize) {
		return create(instances, cacheSize, PRIMARY);
	}
	
	public ActorFactory create(int instances, int cacheSize, AckMode ackMode) {
		ActorGroup group = new ActorGroupSet();
		AtomicInteger k = new AtomicInteger(0);
		return () -> new PrimaryVolatileCacheActor<K, V>(
				"primary-"+cacheAlias, group, cacheAlias, (id) -> () -> new SecondaryVolatileCacheActor<K, V>("secondary-"+cacheAlias+"-"+k.getAndIncrement(), group, id, cacheSize), instances-1, cacheSize, ackMode);		
	}
	
	public static ActorFactory create(int instances, int cacheSize, String cacheAlias) {
		return create(instances, cacheSize, cacheAlias, PRIMARY);
	}
	
	public static ActorFactory create(int instances, int cacheSize, String cacheAlias, AckMode ackMode) {
		ActorGroup group = new ActorGroupSet();
		AtomicInteger k = new AtomicInteger(0);
		return () -> new PrimaryVolatileCacheActor<Object, Object>(
				"primary-"+cacheAlias, group, cacheAlias, (id) -> () -> new SecondaryVolatileCacheActor<Object, Object>("secondary-"+cacheAlias+"-"+k.getAndIncrement(), group, id, cacheSize), instances-1, cacheSize, ackMode);		
	}
	
	@SuppressWarnings("unchecked")
	public Pair<K, V> get(ActorMessage<?> message) {	
		if (message.tag()==GET && message.value()!=null && message.value() instanceof VolatileDTO) {
			VolatileDTO<K, V> dto = (VolatileDTO<K, V>)message.value();
			return Pair.of(dto.key(), dto.value());
		}
		else
			return null;
	}
	
	public void get(K key) {
		actorRef.tell(VolatileDTO.create(key, actorRef.self()), GET, cacheAlias);
	}
	
	public void set(K key, V value) {
		actorRef.tell(VolatileDTO.create(key, value, actorRef.self()), SET, cacheAlias);
	}
	
	public void del(K key) {
		actorRef.tell(VolatileDTO.create(key, actorRef.self()), DEL, cacheAlias);
	}
	
	public void delAll() {
		actorRef.tell(VolatileDTO.create(actorRef.self()), DEL_ALL, cacheAlias);
	}
	
	public void clear() {
		actorRef.tell(VolatileDTO.create(actorRef.self()), CLEAR, cacheAlias);
	}
	
	public void evict(long duration) {
		actorRef.tell(duration, EVICT, cacheAlias);
	}
	
	public void evict(long duration, TimeUnit unit) {
		actorRef.tell(TimeUnit.MILLISECONDS.convert(duration, unit), EVICT, cacheAlias);
	}
}
