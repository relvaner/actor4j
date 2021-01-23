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

import java.util.concurrent.atomic.AtomicInteger;

import io.actor4j.core.actors.ActorRef;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorFactory;
import io.actor4j.core.utils.ActorGroup;
import io.actor4j.core.utils.ActorGroupSet;
import io.actor4j.core.utils.Pair;
import io.actor4j.core.data.access.PrimaryVolatileCacheActor;
import io.actor4j.core.data.access.SecondaryVolatileCacheActor;
import io.actor4j.core.data.access.VolatileDataAccessObject;

public class VolatileActorCacheManager<K, V> {
	protected ActorRef actorRef;
	protected String cacheAlias;
	
	public VolatileActorCacheManager(ActorRef actorRef, String cacheAlias) {
		super();
		
		this.actorRef = actorRef;
		this.cacheAlias = cacheAlias;
	}
	
	public ActorFactory create(int instances, int cacheSize) {
		ActorGroup group = new ActorGroupSet();
		AtomicInteger k = new AtomicInteger(0);
		return () -> new PrimaryVolatileCacheActor<K, V>(
				"primary-"+cacheAlias, group, cacheAlias, (id) -> () -> new SecondaryVolatileCacheActor<K, V>("secondary-"+cacheAlias+"-"+k.getAndIncrement(), group, id, cacheSize), instances-1, cacheSize);		
	}
	
	public static ActorFactory create(int instances, int cacheSize, String cacheAlias) {
		ActorGroup group = new ActorGroupSet();
		AtomicInteger k = new AtomicInteger(0);
		return () -> new PrimaryVolatileCacheActor<Object, Object>(
				"primary-"+cacheAlias, group, cacheAlias, (id) -> () -> new SecondaryVolatileCacheActor<Object, Object>("secondary-"+cacheAlias+"-"+k.getAndIncrement(), group, id, cacheSize), instances-1, cacheSize);		
	}
	
	@SuppressWarnings("unchecked")
	public Pair<K, V> get(ActorMessage<?> message) {	
		if (message.value!=null && message.value instanceof VolatileDataAccessObject) {
			VolatileDataAccessObject<K, V> obj = (VolatileDataAccessObject<K, V>)message.value;
			return Pair.of(obj.key, obj.value);
		}
		else
			return null;
	}
	
	public void get(K key) {
		actorRef.tell(new VolatileDataAccessObject<K, V>(key, actorRef.self()), GET, cacheAlias);
	}
	
	public void set(K key, V value) {
		actorRef.tell(new VolatileDataAccessObject<K, V>(key, value), SET, cacheAlias);
	}
	
	public void del(K key) {
		actorRef.tell(new VolatileDataAccessObject<K, V>(key), DEL, cacheAlias);
	}
	
	public void delAll() {
		actorRef.tell(new VolatileDataAccessObject<K, V>(), DEL_ALL, cacheAlias);
	}
	
	public void clear() {
		actorRef.tell(new VolatileDataAccessObject<K, V>(), CLEAR, cacheAlias);
	}
	
	public void evict(long maxTime) {
		actorRef.tell(maxTime, GC, cacheAlias);
	}
}
