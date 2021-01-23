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
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONObject;

import io.actor4j.core.actors.ActorRef;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorFactory;
import io.actor4j.core.utils.ActorGroup;
import io.actor4j.core.utils.ActorGroupSet;
import io.actor4j.core.utils.Pair;
import io.actor4j.core.data.access.PersistentDataAccessObject;
import io.actor4j.core.data.access.PrimaryPersistentCacheActor;
import io.actor4j.core.data.access.SecondaryPersistentCacheActor;

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
	
	public ActorFactory create(int instances, int cacheSize, UUID dataAccess) {
		ActorGroup group = new ActorGroupSet();
		AtomicInteger k = new AtomicInteger(0);
		return () -> new PrimaryPersistentCacheActor<K, V>(
				"primary-"+cacheAlias, group, cacheAlias, (id) -> () -> new SecondaryPersistentCacheActor<K, V>("secondary-"+cacheAlias+"-"+k.getAndIncrement(), group, id, cacheSize), instances-1, cacheSize, dataAccess);		
	}
	
	public static ActorFactory create(int instances, int cacheSize, UUID dataAccess, String cacheAlias, String keyname, String collectionName) {
		ActorGroup group = new ActorGroupSet();
		AtomicInteger k = new AtomicInteger(0);
		return () -> new PrimaryPersistentCacheActor<Object, Object>(
				"primary-"+cacheAlias, group, cacheAlias, (id) -> () -> new SecondaryPersistentCacheActor<Object, Object>("secondary-"+cacheAlias+"-"+k.getAndIncrement(), group, id, cacheSize), instances-1, cacheSize, dataAccess);		
	}
	
	@SuppressWarnings("unchecked")
	public Pair<K, V> get(ActorMessage<?> message) {	
		if (message.value!=null && message.value instanceof PersistentDataAccessObject) {
			PersistentDataAccessObject<K, V> obj = (PersistentDataAccessObject<K, V>)message.value;
			return Pair.of(obj.key, obj.value);
		}
		else
			return null;
	}

	public void get(K key) {
		if (keyname!=null)
			actorRef.tell(new PersistentDataAccessObject<K, V>(key, keyname, collectionName, actorRef.self()), GET, cacheAlias);
		else
			actorRef.tell(new PersistentDataAccessObject<K, V>(key, collectionName, actorRef.self()), GET, cacheAlias);
	}
	
	public void set(K key, V value) {
		if (keyname!=null)
			actorRef.tell(new PersistentDataAccessObject<K, V>(key, value, keyname, collectionName), SET, cacheAlias);
		else
			actorRef.tell(new PersistentDataAccessObject<K, V>(key, value, collectionName), SET, cacheAlias);
	}
	
	public void update(K key, V value, JSONObject update) {
		if (keyname!=null)
			actorRef.tell(new PersistentDataAccessObject<K, V>(key, value, keyname, update!=null ? update.toString() : null, collectionName), SET, cacheAlias);
		else
			actorRef.tell(new PersistentDataAccessObject<K, V>(key, value, null, update!=null ? update.toString() : null, collectionName), SET, cacheAlias);
	}
	
	public void del(K key) {
		if (keyname!=null)
			actorRef.tell(new PersistentDataAccessObject<K, V>(key, null, keyname, collectionName), DEL, cacheAlias);
		else
			actorRef.tell(new PersistentDataAccessObject<K, V>(key, null, collectionName), DEL, cacheAlias);
	}
	
	public void delAll() {
		if (keyname!=null)
			actorRef.tell(new PersistentDataAccessObject<K, V>(null, null, keyname, collectionName), DEL_ALL, cacheAlias);
		else
			actorRef.tell(new PersistentDataAccessObject<K, V>(null, null, collectionName), DEL_ALL, cacheAlias);
	}
	
	public void clear() {
		if (keyname!=null)
			actorRef.tell(new PersistentDataAccessObject<K, V>(null, null, keyname, collectionName), CLEAR, cacheAlias);
		else
			actorRef.tell(new PersistentDataAccessObject<K, V>(null, null, collectionName), CLEAR, cacheAlias);
	}
	
	public void evict(long maxTime) {
		actorRef.tell(maxTime, GC, cacheAlias);
	}
}
