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
import io.actor4j.core.utils.CacheVolatileLRU;
import io.actor4j.core.utils.DeepCopyable;

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
		cache = new CacheVolatileLRU<>(cacheSize);
		
		this.dataAccess = dataAccess;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void receive(ActorMessage<?> message) {
		if (message.value()!=null) {
			if (message.value() instanceof PersistentDataAccessDTO) {
				PersistentDataAccessDTO<K,V> dto = (PersistentDataAccessDTO<K,V>)message.value();
				
				if (message.tag()==GET) {
					V value = cache.get(dto.key());
					if (value!=null) {
						if (value instanceof DeepCopyable)
							value = ((DeepCopyable<V>)value).deepCopy();
						tell(dto.shallowCopy(value), GET, dto.source(), message.interaction());
					}
					else
						tell(message.value(), GET, dataAccess, message.interaction());
				}
				else if (message.tag()==SET) {
					Object reserved = cache.get(dto.key()) != null;
					cache.put(dto.key(), dto.value());
					tell(dto.shallowCopyWithReserved(reserved), SET, dataAccess);
					publish(VolatileDTO.create(dto.key(), dto.value()), SET);
				}
				else if (message.tag()==UPDATE) {
					cache.remove(dto.key());
					tell(message.value(), UPDATE, dataAccess);
					publish(VolatileDTO.create(dto.key()), DEL);
				}
				else if (message.tag()==DEL) {
					cache.remove(dto.key());
					tell(message.value(), DELETE_ONE, dataAccess);
					publish(VolatileDTO.create(dto.key()), DEL);
				}
				else if (message.tag()==DEL_ALL ) {
					cache.clear();
					// drop collection
					publish(VolatileDTO.create(), DEL_ALL);
				}
				else if (message.tag()==CLEAR) {
					cache.clear();
					publish(VolatileDTO.create(), CLEAR);
				}
				else if (message.source()==dataAccess && message.tag()==FIND_ONE) {
					cache.put(dto.key(), dto.value());
					tell(dto, GET, dto.source(), message.interaction());
					publish(VolatileDTO.create(dto.key(), dto.value()), SET);
				}
				else if (message.tag()==CAS || message.tag()==CAU) {
					V value = cache.get(dto.key());
					if (value.hashCode()==dto.hashCodeExpected()) {
						int tag = SET;
						if (message.tag()==CAU)
							tag = UPDATE;
						receive(message.shallowCopy(tag));
					}
					else
						tell(dto, message.tag(), dto.source(), message.interaction());
				}
				else
					unhandled(message);
			}
			else 
				unhandled(message);
		}
		else if (message.tag()==SUBSCRIBE_SECONDARY)
			hub.add(message.source());
		else if (message.tag()==EVICT) {
			cache.evict(message.valueAsLong());
			publish(null, EVICT);
		}
		else
			unhandled(message);
	}
}
