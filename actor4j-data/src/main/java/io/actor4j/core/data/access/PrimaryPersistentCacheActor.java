/*
 * Copyright (c) 2015-2017, David A. Bauer. All rights reserved.
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
package io.actor4j.core.data.access;

import io.actor4j.core.actors.PrimaryActor;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorFactory;
import io.actor4j.core.utils.ActorGroup;
import io.actor4j.core.utils.Cache;
import io.actor4j.core.utils.CacheLRUWithGC;

import java.util.UUID;
import java.util.function.Function;

import static io.actor4j.core.actors.ActorWithCache.*;
import static io.actor4j.core.data.access.DataAccessActor.*;

public class PrimaryPersistentCacheActor<K, V> extends PrimaryActor {
	protected int cacheSize;
	protected Cache<K, V> cache;
	
	protected UUID dataAccess;
	
	public PrimaryPersistentCacheActor(ActorGroup group, String alias, Function<UUID, ActorFactory> secondary, int instances, int cacheSize, UUID dataAccess) {
		this(null, group, alias, secondary, instances, cacheSize, dataAccess);
	}

	public PrimaryPersistentCacheActor(String name, ActorGroup group, String alias, Function<UUID, ActorFactory> secondary, int instances, int cacheSize, UUID dataAccess) {
		super(name, group, alias, secondary, instances);
		
		this.cacheSize = cacheSize;
		cache = new CacheLRUWithGC<>(cacheSize);
		
		this.dataAccess = dataAccess;
	}
	
	@Override
	public void receive(ActorMessage<?> message) {
		if (message.value!=null) {
			if (message.value instanceof PersistentDataAccessObject) {
				@SuppressWarnings("unchecked")
				PersistentDataAccessObject<K,V> obj = (PersistentDataAccessObject<K,V>)message.value;
				
				if (message.tag==GET) {
					obj.value = cache.get(obj.key);
					if (obj.value!=null)
						tell(obj, GET, obj.source, message.interaction); // normally deep copy necessary of obj.value
					else
						tell(message.value, GET, dataAccess, message.interaction);
				}
				else if (message.tag==SET) {
					obj.reserved = cache.get(obj.key) != null;
					cache.put(obj.key, obj.value);
					tell(message.value, SET, dataAccess);
					publish(new VolatileDataAccessObject<K,V>(obj.key, obj.value, null), SET);
				}
				else if (message.tag==UPDATE) {
					cache.remove(obj.key);
					tell(message.value, UPDATE, dataAccess);
					publish(new VolatileDataAccessObject<K,V>(obj.key, null, null), DEL);
				}
				else if (message.tag==DEL) {
					cache.remove(obj.key);
					tell(message.value, DELETE_ONE, dataAccess);
					publish(new VolatileDataAccessObject<K,V>(obj.key, null, null), DEL);
				}
				else if (message.tag==DEL_ALL ) {
					cache.clear();
					// drop collection
					publish(new VolatileDataAccessObject<K,V>(), DEL_ALL);
				}
				else if (message.tag==CLEAR) {
					cache.clear();
					publish(new VolatileDataAccessObject<K,V>(), CLEAR);
				}
				else if (message.source==dataAccess && message.tag==FIND_ONE) {
					cache.put(obj.key, obj.value);
					tell(obj, GET, obj.source, message.interaction);
					publish(new VolatileDataAccessObject<K,V>(obj.key, obj.value, null), SET);
				}
				else if (message.tag==CAS || message.tag==CAU) {
					V v = cache.get(obj.key);
					if (v.hashCode()==obj.hashCodeExpected) {
						if (message.tag==CAS)
							message.tag = SET;
						else
							message.tag = UPDATE;
						receive(message);
					}
					else
						tell(obj, message.tag, obj.source, message.interaction);
				}
				else
					unhandled(message);
			}
			else 
				unhandled(message);
		}
		else if (message.tag==SUBSCRIBE_SECONDARY)
			hub.add(message.source);
		else if (message.tag==GC) {
			cache.gc(message.valueAsLong());
			publish(null, GC);
		}
		else
			unhandled(message);
	}
}
