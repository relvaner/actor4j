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

import org.json.JSONObject;

import io.actor4j.core.actors.ActorRef;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorFactory;
import io.actor4j.core.utils.ActorGroupSet;
import io.actor4j.core.utils.Pair;
import io.actor4j.core.pods.PodContext;
import io.actor4j.core.pods.data.access.PodPrimaryPersistentCacheActor;
import io.actor4j.core.pods.data.access.PodSecondaryPersistentCacheActor;
import io.actor4j.core.data.access.PersistentDataAccessObject;

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
		if (context.isShard())
			cacheAlias = cacheAlias + context.shardId();
		
		if (context.primaryReplica())
			return () -> new PodPrimaryPersistentCacheActor<K, V>("primary-"+cacheAlias, new ActorGroupSet(), cacheAlias, null, 0, cacheSize, dataAccess, context) {
				@Override
				public void preStart() {
					super.preStart();
					UUID redirect = UUID.randomUUID();
					getSystem().underlyingImpl().setAlias(redirect, cacheAlias+"-primary"); // workaround for setting second alias
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
						primary = getSystem().underlyingImpl().getActorFromAlias(cacheAlias+"-primary");
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
		if (message.value()!=null && message.value() instanceof PersistentDataAccessObject) {
			PersistentDataAccessObject<K, V> obj = (PersistentDataAccessObject<K, V>)message.value();
			return Pair.of(obj.key, obj.value);
		}
		else
			return null;
	}

	public void get(K key) {
		if (keyname!=null)
			actorRef.tell(new PersistentDataAccessObject<K, V>(key, keyname, collectionName, actorRef.self()), GET, replica);
		else
			actorRef.tell(new PersistentDataAccessObject<K, V>(key, collectionName, actorRef.self()), GET, replica);
	}
	
	public void get(K key, UUID interaction) {
		if (keyname!=null)
			actorRef.tell(new PersistentDataAccessObject<K, V>(key, keyname, collectionName, actorRef.self()), GET, replica, interaction);
		else
			actorRef.tell(new PersistentDataAccessObject<K, V>(key, collectionName, actorRef.self()), GET, replica, interaction);
	}
	
	public void set(K key, V value) {
		if (keyname!=null)
			actorRef.tell(new PersistentDataAccessObject<K, V>(key, value, keyname, collectionName), SET, replica);
		else
			actorRef.tell(new PersistentDataAccessObject<K, V>(key, value, collectionName), SET, replica);
	}
	
	public void update(K key, V value, JSONObject update) {
		if (keyname!=null)
			actorRef.tell(new PersistentDataAccessObject<K, V>(key, value, keyname, update!=null ? update.toString() : null, collectionName), SET, replica);
		else
			actorRef.tell(new PersistentDataAccessObject<K, V>(key, value, null, update!=null ? update.toString() : null, collectionName), SET, replica);
	}
	
	public void del(K key) {
		if (keyname!=null)
			actorRef.tell(new PersistentDataAccessObject<K, V>(key, null, keyname, collectionName), DEL, replica);
		else
			actorRef.tell(new PersistentDataAccessObject<K, V>(key, null, collectionName), DEL, replica);
	}
	
	public void delAll() {
		if (keyname!=null)
			actorRef.tell(new PersistentDataAccessObject<K, V>(null, null, keyname, collectionName), DEL_ALL, replica);
		else
			actorRef.tell(new PersistentDataAccessObject<K, V>(null, null, collectionName), DEL_ALL, replica);
	}
	
	public void clear() {
		if (keyname!=null)
			actorRef.tell(new PersistentDataAccessObject<K, V>(null, null, keyname, collectionName), CLEAR, replica);
		else
			actorRef.tell(new PersistentDataAccessObject<K, V>(null, null, collectionName), CLEAR, replica);
	}
	
	public void evict(long maxTime) {
		actorRef.tell(maxTime, GC, replica);
	}
}
