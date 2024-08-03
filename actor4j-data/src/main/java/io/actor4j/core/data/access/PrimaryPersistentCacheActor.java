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

import io.actor4j.core.actors.ActorWithCache;
import io.actor4j.core.actors.PrimaryActor;
import io.actor4j.core.data.access.cache.AsyncCache;
import io.actor4j.core.data.access.cache.AsyncCacheVolatileLRU;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorFactory;
import io.actor4j.core.utils.ActorGroup;
import io.actor4j.core.utils.ActorOptional;
import io.actor4j.core.utils.Cache;
import io.actor4j.core.utils.DeepCopyable;

import java.util.UUID;
import java.util.function.Function;

import static io.actor4j.core.actors.ActorWithCache.*;
import static io.actor4j.core.data.access.AckMode.ALL;
import static io.actor4j.core.data.access.AckMode.PRIMARY;
import static io.actor4j.core.data.access.DataAccessActor.*;

public class PrimaryPersistentCacheActor<K, V> extends PrimaryActor {
	protected int cacheSize;
	protected Cache<K, V> cache;
	
	protected UUID dataAccess;
	protected AckMode ackMode;
	
	protected AckWatcher<K> getWatcher;
	protected AckWatcher<K> delWatcher;
	
	public PrimaryPersistentCacheActor(ActorGroup group, String alias, Function<UUID, ActorFactory> secondary, int instances, int cacheSize, UUID dataAccess, AckMode ackMode) {
		this(null, group, alias, secondary, instances, cacheSize, dataAccess, ackMode);
	}

	public PrimaryPersistentCacheActor(String name, ActorGroup group, String alias, Function<UUID, ActorFactory> secondary, int instances, int cacheSize, UUID dataAccess, AckMode ackMode) {
		super(name, group, alias, secondary, instances);
		
		this.cacheSize = cacheSize;
		cache = new AsyncCacheVolatileLRU<>(cacheSize);
		
		this.dataAccess = dataAccess;
		this.ackMode = ackMode;
		
		getWatcher = new AckWatcher<>();
		delWatcher = new AckWatcher<>();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void receive(ActorMessage<?> message) {
		if (message.value()!=null && message.value() instanceof PersistentDataAccessDTO) {
			PersistentDataAccessDTO<K,V> dto = (PersistentDataAccessDTO<K,V>)message.value();
			
			try {
				boolean unhandled = false;
				if (message.tag()==GET) {
					ActorOptional<V> optional = ((AsyncCache<K,V>)cache).get(dto.key(), 
						() -> tell(message.value(), GET, dataAccess, message.interaction()),
						() -> getWatcher.watch(dto.key(), dto.source(), message.interaction()),
						() -> tell(dto/*value==null*/, GET, dto.source(), message.interaction())
					);
					if (optional.isPresent()) {
						V value = optional.get();
						if (value!=null) {
							if (value instanceof DeepCopyable)
								value = ((DeepCopyable<V>)value).deepCopy();
							tell(dto.shallowCopy(value), GET, dto.source(), message.interaction());
						}
						else
							tell(dto, GET, dto.source(), message.interaction());
					}
				}
				else if (message.tag()==SET) {
					Object reserved = cache.get(dto.key()) != null;
					cache.put(dto.key(), dto.value());
					tell(dto.shallowCopyWithReserved(reserved), SET, dataAccess);
					publish(VolatileDTO.create(dto.key(), dto.value(), dto.source()), SET);
				}
				else if (message.tag()==UPDATE) {
					((AsyncCache<K,V>)cache).remove(dto.key(), () -> publish(VolatileDTO.create(dto.key(), dto.source()), DEL), null);
					tell(message.value(), UPDATE, dataAccess);
				}
				else if (message.tag()==DEL) {
					((AsyncCache<K,V>)cache).remove(dto.key(), 
						() -> {
							tell(message.value(), DELETE_ONE, dataAccess);
							publish(VolatileDTO.create(dto.key(), dto.source()), DEL);
						},
						() -> delWatcher.watch(dto.key(), dto.source(), message.interaction())
						);
				}
				else if (message.tag()==DEL_ALL ) {
					cache.clear();
					// drop collection, not implemented
					publish(VolatileDTO.create(dto.source()), DEL_ALL);
				}
				else if (message.tag()==CLEAR) {
					cache.clear();
					publish(VolatileDTO.create(dto.source()), CLEAR);
				}
				else if ((message.tag()==FIND_ONE || message.tag()==FIND_NONE) && message.source()==dataAccess) {
					if (message.tag()==FIND_ONE)
						cache.put(dto.key(), dto.value());
					tell(dto, GET, dto.source(), message.interaction());
					getWatcher.trigger(dto.key(), (source, interaction) -> tell(dto.shallowCopy(source), GET, source, interaction));
					if (message.tag()==FIND_ONE)
						publish(VolatileDTO.create(dto.key(), dto.value(), dto.source()), SET);
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
				else {
					unhandled = true;
					unhandled(message);
				}
				
				if (!unhandled) {
					if (message.tag()==CLEAR && (ackMode==PRIMARY || ackMode==ALL))
						tell(PersistentSuccessDTO.of(dto, message.tag()), ActorWithCache.SUCCESS, dto.source(), message.interaction());
				}
				else
					tell(dto, ActorMessage.UNHANDLED, dto.source(), message.interaction());
			}
			catch(Exception e) {
				e.printStackTrace();
				
				tell(PersistentFailureDTO.of(dto, message.tag(), e), ActorWithCache.FAILURE, dto.source(), message.interaction());
			}
			
		}
		else if (message.tag()==DataAccessActor.SUCCESS && message.source()==dataAccess && message.value() instanceof PersistentSuccessDTO success)
			handleSuccess(message, success);
		else if (message.tag()==DataAccessActor.FAILURE && message.source()==dataAccess && message.value() instanceof PersistentFailureDTO failure)
			handleFailure(message, failure);
		else if (message.tag()==SUBSCRIBE_SECONDARY)
			hub.add(message.source());
		else if (message.tag()==EVICT) {
			cache.evict(message.valueAsLong());
			publish(null, EVICT);
		}
		else
			unhandled(message);
	}
	
	public void handleSuccess(ActorMessage<?> message, PersistentSuccessDTO<K,V> success) {
		((AsyncCache<K,V>)cache).complete(success.tag(), success.dto().key(), success.dto().value());
		if (ackMode==PRIMARY || ackMode==ALL)
			tell(success, DataAccessActor.SUCCESS, success.dto().source(), message.interaction());
	}
	
	public void handleFailure(ActorMessage<?> message, PersistentFailureDTO<K,V> failure) {
		tell(failure, DataAccessActor.FAILURE, failure.dto().source(), message.interaction());
	}
}
