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

import static io.actor4j.core.actors.ActorWithCache.*;

import java.util.UUID;

import io.actor4j.core.actors.Actor;
import io.actor4j.core.actors.ActorRef;
import io.actor4j.core.utils.ActorFactory;
import io.actor4j.core.utils.ActorGroupSet;
import io.actor4j.core.pods.PodContext;
import io.actor4j.core.pods.data.access.PodPrimaryPersistentCacheActor;
import io.actor4j.core.pods.data.access.PodSecondaryPersistentCacheActor;
import io.actor4j.core.data.access.AckMode;
import io.actor4j.core.data.access.DataAccessType;
import io.actor4j.core.data.access.DocPersistentContext;
import io.actor4j.core.data.access.PersistentDTO;
import io.actor4j.core.data.access.utils.PersistentActorCacheManager;
import io.actor4j.core.id.ActorId;

import static io.actor4j.core.data.access.AckMode.*;
import static io.actor4j.core.data.access.DataAccessType.DOC;

public class PodPersistentActorCacheManager<K, V> extends PersistentActorCacheManager<K, V> {
	public static final UUID PRIMARY_FROM_CACHE_COORDINATOR = UUID.randomUUID();
	
	protected UUID groupId;
	
	public PodPersistentActorCacheManager(ActorRef actorRef, String cacheAlias, String keyname, String collectionName, UUID groupId) {
		super(actorRef, cacheAlias, keyname, collectionName);
		this.groupId = groupId;
	}
	
	public PodPersistentActorCacheManager(ActorRef actorRef, String cacheAlias, DataAccessType dataAccessType, UUID groupId) {
		super(actorRef, cacheAlias, dataAccessType);
		this.groupId = groupId;
	}
	
	@Deprecated
	public ActorFactory create(int instances, int cacheSize, ActorId dataAccess) {
		throw new UnsupportedOperationException();
	}
	
	@Deprecated
	public ActorFactory create(int instances, int cacheSize, ActorId dataAccess, AckMode ackMode) {
		throw new UnsupportedOperationException();
	}
	
	public ActorFactory createReplicaAsActorFactory(int cacheSize, ActorId dataAccess, AckMode ackMode, PodContext context) {
		this.dataAccess = dataAccess;
		
		if (context.isShard())
			cacheAlias = cacheAlias + context.shardId();
		
		if (context.primaryReplica())
			return () -> new PodPrimaryPersistentCacheActor<K, V>("primary-"+cacheAlias, new ActorGroupSet(), cacheAlias, null, 0, cacheSize, dataAccess, ackMode, context) {
				@Override
				public void preStart() {
					super.preStart();
					UUID redirect = UUID.randomUUID();
					getSystem().setAlias(redirect, cacheAlias+"-primary"); // workaround for setting second alias
					getSystem().addRedirection(redirect, self());
				}
				
				@Override
				public UUID getGroupId() {
					return PodPersistentActorCacheManager.this.groupId;
				}
			};
		else
			return () -> new PodSecondaryPersistentCacheActor<K, V>("secondary-"+cacheAlias, new ActorGroupSet(), cacheAlias, /*id*/null, cacheSize, context) {
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
					return PodPersistentActorCacheManager.this.groupId;
				}
			};
	}
	
	public ActorId replica() {
		return replica;
	}
	
	public void replica(ActorId replica) {
		this.replica = replica;
	}
	
	public ActorId createReplica(int cacheSize, ActorId dataAccess, PodContext context) {
		return replica = ((Actor)actorRef).addChild(createReplicaAsActorFactory(cacheSize, dataAccess, PRIMARY, context));
	}
	
	public ActorId createReplica(int cacheSize, ActorId dataAccess, AckMode ackMode, PodContext context) {
		return replica = ((Actor)actorRef).addChild(createReplicaAsActorFactory(cacheSize, dataAccess, ackMode, context));
	}
	
	public void get(K key, UUID interaction) {
		if (dataAccessType==DOC) {
			if (keyname!=null)
				actorRef.tell(PersistentDTO.create(key, DocPersistentContext.of(keyname, collectionName), actorRef.self()), GET, replica, interaction);
		}
		else
			actorRef.tell(PersistentDTO.create(key, null, actorRef.self()), GET, replica, interaction);
	}
}
