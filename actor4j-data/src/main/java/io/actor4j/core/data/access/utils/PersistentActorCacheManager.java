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
import io.actor4j.core.data.access.DataAccessActor;
import io.actor4j.core.data.access.DocPersistentContext;
import io.actor4j.core.data.access.DataAccessType;
import io.actor4j.core.data.access.PersistentDTO;
import io.actor4j.core.data.access.PrimaryPersistentCacheActor;
import io.actor4j.core.data.access.SecondaryPersistentCacheActor;
import io.actor4j.core.data.access.SqlPersistentContext;
import io.actor4j.core.json.JsonObject;
import static io.actor4j.core.data.access.AckMode.*;
import static io.actor4j.core.data.access.DataAccessType.*;

public class PersistentActorCacheManager<K, V> {
	protected ActorRef actorRef;
	protected String cacheAlias;

	protected UUID dataAccess;
	protected DataAccessType dataAccessType;
	
	protected String keyname;
	protected String collectionName;
	
	protected UUID replica;
	
	public PersistentActorCacheManager(ActorRef actorRef, String cacheAlias, String keyname, String collectionName) {
		super();
		
		this.actorRef = actorRef;
		this.cacheAlias = cacheAlias;
		this.dataAccessType = DOC;
		
		this.keyname = keyname;
		this.collectionName = collectionName;
	}
	
	public PersistentActorCacheManager(ActorRef actorRef, String cacheAlias, DataAccessType dataAccessType) {
		super();
		
		this.actorRef = actorRef;
		this.cacheAlias = cacheAlias;
		this.dataAccessType = dataAccessType;
	}
	
	public ActorFactory create(int instances, int cacheSize, UUID dataAccess) {
		return create(instances, cacheSize, dataAccess, PRIMARY);
	}
	
	public ActorFactory create(int instances, int cacheSize, UUID dataAccess, AckMode ackMode) {
		this.dataAccess = dataAccess;
		
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
			return Pair.of(dto.key(), (V)dto.value());
		}
		else
			return null;
	}
	
	protected void tell(Object value, int tag) {
		if (replica!=null)
			actorRef.tell(value, tag, replica);
		else
			actorRef.tell(value, tag, cacheAlias);
	}

	public void get(K key) {
		if (dataAccessType==DOC) {
			if (keyname!=null)
				tell(PersistentDTO.create(key, DocPersistentContext.of(keyname, collectionName), actorRef.self()), GET);
		}
		else
			tell(PersistentDTO.create(key, null, actorRef.self()), GET);
	}
	
	public void set(K key, V value) {
		if (dataAccessType==DOC) {
			if (keyname!=null)
				tell(PersistentDTO.create(key, value, DocPersistentContext.of(keyname, collectionName), actorRef.self()), SET);
		}
		else
			tell(PersistentDTO.create(key, value, actorRef.self()), SET);
	}
	
	public void writeAround(K key, V value) {
		if (dataAccessType==DOC) {
			if (keyname!=null)
				actorRef.tell(PersistentDTO.create(key, value, DocPersistentContext.of(keyname, collectionName), actorRef.self(), false), SET, dataAccess);
		}
		else
			actorRef.tell(PersistentDTO.create(key, value, actorRef.self(), false), SET, dataAccess);
	}
	
	public void update(K key, V value, JsonObject update) {
		if (dataAccessType==DOC) {
			if (keyname!=null)
				tell(PersistentDTO.create(key, value, DocPersistentContext.of(keyname, update, collectionName), actorRef.self()), SET);
		}
		else
			tell(PersistentDTO.create(key, value, actorRef.self()), SET);
	}
	
	public void writeAround(K key, V value, JsonObject update) {
		if (dataAccessType==DOC) {
			if (keyname!=null)
				actorRef.tell(PersistentDTO.create(key, value, DocPersistentContext.of(keyname, update, collectionName), actorRef.self(), false), SET, dataAccess);
		}
		else
			actorRef.tell(PersistentDTO.create(key, value, actorRef.self(), false), SET, dataAccess);
	}
	
	public void compareAndSet(K key, V value) {
		if (dataAccessType==DOC) {
			if (keyname!=null)
				tell(PersistentDTO.create(key, value, DocPersistentContext.of(keyname, collectionName), actorRef.self()), CAS);
		}
		else
			tell(PersistentDTO.create(key, value, actorRef.self()), CAS);
	}
	
	public void compareAndUpdate(K key, V value) {
		if (dataAccessType==DOC) {
			if (keyname!=null)
				tell(PersistentDTO.create(key, value, DocPersistentContext.of(keyname, collectionName), actorRef.self()), CAU);
		}
		else
			tell(PersistentDTO.create(key, value, actorRef.self()), CAU);
	}
	
	public void queryOne(String query) {
		if (dataAccessType==SQL)
			actorRef.tell(PersistentDTO.create(SqlPersistentContext.of(query), actorRef.self()), DataAccessActor.QUERY_ONE, dataAccess);
	}
	
	public void queryAll(String query) {
		if (dataAccessType==SQL)
			actorRef.tell(PersistentDTO.create(SqlPersistentContext.of(query), actorRef.self()), DataAccessActor.QUERY_ALL, dataAccess);
	}
	
	public void del(K key) {
		if (dataAccessType==DOC) {
			if (keyname!=null)
				tell(PersistentDTO.create(key, null, DocPersistentContext.of(keyname, collectionName), actorRef.self()), DEL);
		}
		else
			tell(PersistentDTO.create(key, null, actorRef.self()), DEL);
	}
	
	public void delAll() {
		if (dataAccessType==DOC)
			tell(PersistentDTO.create(null, null, DocPersistentContext.of(collectionName), actorRef.self()), DEL_ALL);
		else
			tell(PersistentDTO.create(null, null, actorRef.self()), DEL_ALL);
	}
	
	public void clear() {
		tell(PersistentDTO.create(null, null, actorRef.self()), CLEAR);
	}
	
	public void evict(long duration) {
		tell(duration, EVICT);
	}
	
	public void evict(long duration, TimeUnit unit) {
		tell(TimeUnit.MILLISECONDS.convert(duration, unit), EVICT);
	}
	
	public void syncWithStorage() {
		if (dataAccessType==DOC)
			tell(PersistentDTO.create(null, null, DocPersistentContext.of(keyname, collectionName), actorRef.self()), SYNC_WITH_STORAGE);
		else
			tell(PersistentDTO.create(null, null, actorRef.self()), SYNC_WITH_STORAGE);
	}
}
