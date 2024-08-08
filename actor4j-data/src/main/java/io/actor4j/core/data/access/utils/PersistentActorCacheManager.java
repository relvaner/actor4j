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

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.actor4j.core.actors.ActorRef;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorFactory;
import io.actor4j.core.utils.ActorGroup;
import io.actor4j.core.utils.ActorGroupSet;
import io.actor4j.core.utils.Pair;
import io.actor4j.core.data.access.AckMode;
import io.actor4j.core.data.access.PersistentDTO;
import io.actor4j.core.data.access.PrimaryPersistentCacheActor;
import io.actor4j.core.data.access.SecondaryPersistentCacheActor;
import io.actor4j.core.json.JsonObject;
import static io.actor4j.core.data.access.AckMode.*;

public class PersistentActorCacheManager<K, V> {
	protected ActorRef actorRef;
	protected String cacheAlias;
	protected String keyname;
	protected String collectionName;
	
	public PersistentActorCacheManager(ActorRef actorRef, String cacheAlias, String keyname, String collectionName) {
		super();
		
		this.actorRef = actorRef;
		this.cacheAlias = cacheAlias;
		this.keyname = keyname;
		this.collectionName = collectionName;
	}
	
	public PersistentActorCacheManager(ActorRef actorRef, String cacheAlias) {
		this(actorRef, cacheAlias, null, null);
	}
	
	public ActorFactory create(int instances, int cacheSize, UUID dataAccess) {
		return create(instances, cacheSize, dataAccess, PRIMARY);
	}
	
	public ActorFactory create(int instances, int cacheSize, UUID dataAccess, AckMode ackMode) {
		ActorGroup group = new ActorGroupSet();
		AtomicInteger k = new AtomicInteger(0);
		return () -> new PrimaryPersistentCacheActor<K, V>(
				"primary-"+cacheAlias, group, cacheAlias, (id) -> () -> new SecondaryPersistentCacheActor<K, V>("secondary-"+cacheAlias+"-"+k.getAndIncrement(), group, id, cacheSize), instances-1, cacheSize, dataAccess, ackMode);		
	}
	
	public static ActorFactory create(int instances, int cacheSize, UUID dataAccess, String cacheAlias, String keyname, String collectionName, AckMode ackMode) {
		ActorGroup group = new ActorGroupSet();
		AtomicInteger k = new AtomicInteger(0);
		return () -> new PrimaryPersistentCacheActor<Object, Object>(
				"primary-"+cacheAlias, group, cacheAlias, (id) -> () -> new SecondaryPersistentCacheActor<Object, Object>("secondary-"+cacheAlias+"-"+k.getAndIncrement(), group, id, cacheSize), instances-1, cacheSize, dataAccess, ackMode);		
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
			actorRef.tell(PersistentDTO.create(key, keyname, collectionName, actorRef.self()), GET, cacheAlias);
		else
			actorRef.tell(PersistentDTO.create(key, collectionName, actorRef.self()), GET, cacheAlias);
	}
	
	public void set(K key, V value) {
		if (keyname!=null)
			actorRef.tell(PersistentDTO.create(key, value, keyname, collectionName, actorRef.self()), SET, cacheAlias);
		else
			actorRef.tell(PersistentDTO.create(key, value, collectionName, actorRef.self()), SET, cacheAlias);
	}
	
	public void update(K key, V value, JsonObject update) {
		if (keyname!=null)
			actorRef.tell(PersistentDTO.create(key, value, keyname, update!=null ? update : null, collectionName, actorRef.self()), SET, cacheAlias);
		else
			actorRef.tell(PersistentDTO.create(key, value, keyname, update!=null ? update : null, collectionName, actorRef.self()), SET, cacheAlias);
	}
	
	public void del(K key) {
		if (keyname!=null)
			actorRef.tell(PersistentDTO.create(key, null, keyname, collectionName, actorRef.self()), DEL, cacheAlias);
		else
			actorRef.tell(PersistentDTO.create(key, null, collectionName, actorRef.self()), DEL, cacheAlias);
	}
	
	public void delAll() {
		if (keyname!=null)
			actorRef.tell(PersistentDTO.create(null, null, keyname, collectionName, actorRef.self()), DEL_ALL, cacheAlias);
		else
			actorRef.tell(PersistentDTO.create(null, null, collectionName, actorRef.self()), DEL_ALL, cacheAlias);
	}
	
	public void clear() {
		if (keyname!=null)
			actorRef.tell(PersistentDTO.create(null, null, keyname, collectionName, actorRef.self()), CLEAR, cacheAlias);
		else
			actorRef.tell(PersistentDTO.create(null, null, collectionName, actorRef.self()), CLEAR, cacheAlias);
	}
	
	public void evict(long duration) {
		actorRef.tell(duration, EVICT, cacheAlias);
	}
	
	public void evict(long duration, TimeUnit unit) {
		actorRef.tell(TimeUnit.MILLISECONDS.convert(duration, unit), EVICT, cacheAlias);
	}
	
	public void syncWithStorage() {
		if (keyname!=null)
			actorRef.tell(PersistentDTO.create(null, null, keyname, collectionName, actorRef.self()), SYNC_WITH_STORAGE, cacheAlias);
		else
			actorRef.tell(PersistentDTO.create(null, null, collectionName, actorRef.self()), SYNC_WITH_STORAGE, cacheAlias);
	}
}
