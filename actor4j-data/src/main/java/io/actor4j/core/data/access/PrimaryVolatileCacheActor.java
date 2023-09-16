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

import static io.actor4j.core.actors.ActorWithCache.*;

import java.util.UUID;
import java.util.function.Function;

import io.actor4j.core.actors.PrimaryActor;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorFactory;
import io.actor4j.core.utils.ActorGroup;
import io.actor4j.core.utils.Cache;
import io.actor4j.core.utils.CacheLRUWithExpiration;
import io.actor4j.core.utils.DeepCopyable;

public class PrimaryVolatileCacheActor<K, V> extends PrimaryActor {
	protected int cacheSize;
	protected Cache<K, V> cache;
	
	public PrimaryVolatileCacheActor(ActorGroup group, String alias, Function<UUID, ActorFactory> secondary, int instances, int cacheSize) {
		this(null, group, alias, secondary, instances, cacheSize);
	}

	public PrimaryVolatileCacheActor(String name, ActorGroup group, String alias, Function<UUID, ActorFactory> secondary, int instances, int cacheSize) {
		super(name, group, alias, secondary, instances);
		
		this.cacheSize = cacheSize;
		cache = new CacheLRUWithExpiration<>(cacheSize);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void receive(ActorMessage<?> message) {
		if (message.value()!=null && message.value() instanceof VolatileDataAccessDTO) {
			VolatileDataAccessDTO<K,V> dto = (VolatileDataAccessDTO<K,V>)message.value();
			
			if (message.tag()==GET) {
				V value = cache.get(dto.key());
				if (value instanceof DeepCopyable)
					value = ((DeepCopyable<V>)value).deepCopy();
				tell(dto.shallowCopy(value), GET, dto.source(), message.interaction());
			}
			else if (message.tag()==SET) {
				cache.put(dto.key(), dto.value());
				publish(VolatileDTO.create(dto.key(), dto.value()), SET);
			}
			else if (message.tag()==UPDATE)
				; // empty
			else if (message.tag()==DEL) {
				cache.remove(dto.key());
				publish(VolatileDTO.create(dto.key()), DEL);
			}
			else if (message.tag()==DEL_ALL || message.tag()==CLEAR) {
				cache.clear();
				publish(VolatileDTO.create(), DEL_ALL);
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
