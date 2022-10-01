/*
 * Copyright (c) 2015-2020, David A. Bauer. All rights reserved.
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
package io.actor4j.core.pods.data.access.utils;

import static io.actor4j.core.actors.ActorWithCache.CLEAR;
import static io.actor4j.core.actors.ActorWithCache.DEL;
import static io.actor4j.core.actors.ActorWithCache.DEL_ALL;
import static io.actor4j.core.actors.ActorWithCache.GC;
import static io.actor4j.core.actors.ActorWithCache.GET;
import static io.actor4j.core.actors.ActorWithCache.SET;

import java.util.UUID;

import io.actor4j.core.actors.ActorRef;
import io.actor4j.core.data.access.VolatileDTO;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorFactory;
import io.actor4j.core.utils.ActorGroupSet;
import io.actor4j.core.utils.Pair;
import io.actor4j.core.pods.PodContext;
import io.actor4j.core.pods.data.access.PodPrimaryVolatileCacheActor;
import io.actor4j.core.pods.data.access.PodSecondaryVolatileCacheActor;

public class PodVolatileActorCacheManager<K, V> {
	public static final UUID PRIMARY_FROM_CACHE_COORDINATOR = UUID.randomUUID();
	
	protected UUID groupId;
	protected ActorRef actorRef;
	protected String cacheAlias;
	
	protected UUID replica;
	
	public PodVolatileActorCacheManager(ActorRef actorRef, String cacheAlias, UUID groupId) {
		super();
		this.groupId = groupId;
		this.actorRef = actorRef;
		this.cacheAlias = cacheAlias;
	}
	
	public ActorFactory createReplica(int cacheSize, PodContext context) {
		if (context.isShard())
			cacheAlias = cacheAlias + context.shardId();
		
		if (context.primaryReplica())
			return () -> new PodPrimaryVolatileCacheActor<K, V>("primary-"+cacheAlias, new ActorGroupSet(), cacheAlias, null, 0, cacheSize, context) {
				@Override
				public void preStart() {
					super.preStart();
					UUID redirect = UUID.randomUUID();
					getSystem().setAlias(redirect, cacheAlias+"-primary"); // workaround for setting second alias
					getSystem().addRedirection(redirect, self());
				}
				
				@Override
				public UUID getGroupId() {
					return PodVolatileActorCacheManager.this.groupId;
				}
			};
		else
			return () -> new PodSecondaryVolatileCacheActor<K, V>("secondary-"+cacheAlias, new ActorGroupSet(), cacheAlias, /*id*/null, cacheSize, context) {
				@Override
				public void preStart() {
					setAlias(cacheAlias);
					if (context.hasPrimaryReplica()) {
						primary = getSystem().getActorFromAlias(cacheAlias+"-primary");
						primary = getSystem().getRedirectionDestination(primary); // ensures that secondary resolves primary original address
						subscribeAsSecondary();
					}
					else
						primary = PRIMARY_FROM_CACHE_COORDINATOR;
				}
				
				@Override
				public UUID getGroupId() {
					return PodVolatileActorCacheManager.this.groupId;
				}
			};
	}
	
	public void init(UUID replica) {
		this.replica = replica;
	}
	
	@SuppressWarnings("unchecked")
	public Pair<K, V> get(ActorMessage<?> message) {	
		if (message.value()!=null && message.value() instanceof VolatileDTO) {
			VolatileDTO<K, V> dto = (VolatileDTO<K, V>)message.value();
			return Pair.of(dto.key(), dto.value());
		}
		else
			return null;
	}
	
	public void get(K key) {
		actorRef.tell(VolatileDTO.create(key, actorRef.self()), GET, replica);
	}
	
	public void get(K key, UUID intercation) {
		actorRef.tell(VolatileDTO.create(key, actorRef.self()), GET, replica, intercation);
	}
	
	public void set(K key, V value) {
		actorRef.tell(VolatileDTO.create(key, value), SET, replica);
	}
	
	public void del(K key) {
		actorRef.tell(VolatileDTO.create(key), DEL, replica);
	}
	
	public void delAll() {
		actorRef.tell(VolatileDTO.create(), DEL_ALL, replica);
	}
	
	public void clear() {
		actorRef.tell(VolatileDTO.create(), CLEAR, replica);
	}
	
	public void evict(long maxTime) {
		actorRef.tell(maxTime, GC, replica);
	}
}
