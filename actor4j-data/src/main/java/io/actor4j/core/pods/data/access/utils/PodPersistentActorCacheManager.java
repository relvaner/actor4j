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
import java.util.concurrent.TimeUnit;

import io.actor4j.core.actors.ActorRef;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorFactory;
import io.actor4j.core.utils.ActorGroupSet;
import io.actor4j.core.utils.Pair;
import io.actor4j.core.pods.PodContext;
import io.actor4j.core.pods.data.access.PodPrimaryPersistentCacheActor;
import io.actor4j.core.pods.data.access.PodSecondaryPersistentCacheActor;
import io.actor4j.core.data.access.AckMode;
import io.actor4j.core.data.access.PersistentDTO;
import io.actor4j.core.json.JsonObject;
import static io.actor4j.core.data.access.AckMode.*;

public class PodPersistentActorCacheManager<K, V> {
	public static final UUID PRIMARY_FROM_CACHE_COORDINATOR = UUID.randomUUID();
	
	protected UUID groupId;
	protected ActorRef actorRef;
	protected String cacheAlias;
	protected String keyname;
	protected String collectionName;
	
	protected UUID replica;
	
	public PodPersistentActorCacheManager(ActorRef actorRef, String cacheAlias, String keyname, String collectionName, UUID groupId) {
		super();
		
		this.groupId = groupId;
		this.actorRef = actorRef;
		this.cacheAlias = cacheAlias;
		this.keyname = keyname;
		this.collectionName = collectionName;
	}
	
	public ActorFactory createReplica(int cacheSize, UUID dataAccess, PodContext context) {
		return createReplica(cacheSize, dataAccess, PRIMARY, context);
	}
	
	public ActorFactory createReplica(int cacheSize, UUID dataAccess, AckMode ackMode, PodContext context) {
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
	
	public void init(UUID replica) {
		this.replica = replica;
	}
	
	@SuppressWarnings("unchecked")
	public Pair<K, V> get(ActorMessage<?> message) {	
		if (message.tag()==GET && message.value()!=null && message.value() instanceof PersistentDTO) {
			PersistentDTO<K, V> dto = (PersistentDTO<K, V>)message.value();
			return Pair.of(dto.key(), dto.value());
		}
		else
			return null;
	}

	public void get(K key) {
		if (keyname!=null)
			actorRef.tell(PersistentDTO.create(key, keyname, collectionName, actorRef.self()), GET, replica);
		else
			actorRef.tell(PersistentDTO.create(key, collectionName, actorRef.self()), GET, replica);
	}
	
	public void get(K key, UUID interaction) {
		if (keyname!=null)
			actorRef.tell(PersistentDTO.create(key, keyname, collectionName, actorRef.self()), GET, replica, interaction);
		else
			actorRef.tell(PersistentDTO.create(key, collectionName, actorRef.self()), GET, replica, interaction);
	}
	
	public void set(K key, V value) {
		if (keyname!=null)
			actorRef.tell(PersistentDTO.create(key, value, keyname, collectionName, actorRef.self()), SET, replica);
		else
			actorRef.tell(PersistentDTO.create(key, value, collectionName, actorRef.self()), SET, replica);
	}
	
	public void update(K key, V value, JsonObject update) {
		if (keyname!=null)
			actorRef.tell(PersistentDTO.create(key, value, keyname, update!=null ? update : null, collectionName, actorRef.self()), SET, replica);
		else
			actorRef.tell(PersistentDTO.create(key, value, keyname, update!=null ? update : null, collectionName, actorRef.self()), SET, replica);
	}
	
	public void del(K key) {
		if (keyname!=null)
			actorRef.tell(PersistentDTO.create(key, null, keyname, collectionName, actorRef.self()), DEL, replica);
		else
			actorRef.tell(PersistentDTO.create(key, null, collectionName, actorRef.self()), DEL, replica);
	}
	
	public void delAll() {
		if (keyname!=null)
			actorRef.tell(PersistentDTO.create(null, null, keyname, collectionName, actorRef.self()), DEL_ALL, replica);
		else
			actorRef.tell(PersistentDTO.create(null, null, collectionName, actorRef.self()), DEL_ALL, replica);
	}
	
	public void clear() {
		if (keyname!=null)
			actorRef.tell(PersistentDTO.create(null, null, keyname, collectionName, actorRef.self()), CLEAR, replica);
		else
			actorRef.tell(PersistentDTO.create(null, null, collectionName, actorRef.self()), CLEAR, replica);
	}
	
	public void evict(long duration) {
		actorRef.tell(duration, EVICT, replica);
	}
	
	public void evict(long duration, TimeUnit unit) {
		actorRef.tell(TimeUnit.MILLISECONDS.convert(duration, unit), EVICT, replica);
	}
}
