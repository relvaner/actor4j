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

import java.util.UUID;

import io.actor4j.core.actors.Actor;
import io.actor4j.core.actors.ActorRef;
import io.actor4j.core.data.access.AckMode;
import io.actor4j.core.data.access.VolatileDTO;
import io.actor4j.core.data.access.utils.VolatileActorCacheManager;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.id.GlobalId;
import io.actor4j.core.utils.ActorFactory;
import io.actor4j.core.utils.ActorGroupSet;
import io.actor4j.core.pods.PodContext;
import io.actor4j.core.pods.data.access.PodPrimaryVolatileCacheActor;
import io.actor4j.core.pods.data.access.PodSecondaryVolatileCacheActor;

import static io.actor4j.core.actors.ActorWithCache.GET;
import static io.actor4j.core.data.access.AckMode.*;

public class PodVolatileActorCacheManager<K, V> extends VolatileActorCacheManager<K, V> {
	public static final ActorId PRIMARY_FROM_CACHE_COORDINATOR = GlobalId.random();
	
	protected UUID groupId;
	
	public PodVolatileActorCacheManager(ActorRef actorRef, String cacheAlias, UUID groupId) {
		super(actorRef, cacheAlias);
		this.groupId = groupId;
	}
	
	@Deprecated
	public ActorFactory create(int instances, int cacheSize) {
		throw new UnsupportedOperationException();
	}
	
	@Deprecated
	public ActorFactory create(int instances, int cacheSize, AckMode ackMode) {
		throw new UnsupportedOperationException();
	}
	
	public ActorFactory createReplicaAsActorFactory(int cacheSize, AckMode ackMode, PodContext context) {
		if (context.isShard())
			cacheAlias = cacheAlias + context.shardId();
		
		if (context.primaryReplica())
			return () -> new PodPrimaryVolatileCacheActor<K, V>("primary-"+cacheAlias, new ActorGroupSet(), cacheAlias, null, 0, cacheSize, ackMode, context) {
				@Override
				public void preStart() {
					super.preStart();
					ActorId redirect = ActorId.ofRedirect();
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
	
	public ActorId replica() {
		return replica;
	}
	
	public void replica(ActorId replica) {
		this.replica = replica;
	}
	
	public ActorId createReplica(int cacheSize, PodContext context) {
		return replica = ((Actor)actorRef).addChild(createReplicaAsActorFactory(cacheSize, PRIMARY, context));
	}
	
	public ActorId createReplica(int cacheSize, AckMode ackMode, PodContext context) {
		return replica = ((Actor)actorRef).addChild(createReplicaAsActorFactory(cacheSize, ackMode, context));
	}
	
	public void get(K key, UUID interaction) {
		actorRef.tell(VolatileDTO.create(key, actorRef.self()), GET, replica, interaction);
	}
}
