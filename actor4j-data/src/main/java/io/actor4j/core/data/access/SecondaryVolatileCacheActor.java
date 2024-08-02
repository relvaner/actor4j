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

import io.actor4j.core.actors.SecondaryActor;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorGroup;
import io.actor4j.core.utils.Cache;
import io.actor4j.core.utils.CacheVolatileLRU;
import io.actor4j.core.utils.DeepCopyable;

public class SecondaryVolatileCacheActor<K, V> extends SecondaryActor {
	protected int cacheSize;
	protected Cache<K, V> cache;

	public SecondaryVolatileCacheActor(ActorGroup group, UUID primary, int cacheSize) {
		this(null, group, primary, cacheSize);
	}

	public SecondaryVolatileCacheActor(String name, ActorGroup group, UUID primary, int cacheSize) {
		super(name, group, primary);
		
		this.cacheSize = cacheSize;
		cache = new CacheVolatileLRU<>(cacheSize);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void receive(ActorMessage<?> message) {
		if (message.value()!=null && message.value() instanceof VolatileDataAccessDTO) {
			VolatileDataAccessDTO<K,V> dto = (VolatileDataAccessDTO<K,V>)message.value();
			
			try {
				boolean unhandled = false;
				if (message.tag()==GET) {
					V value = cache.get(dto.key());
					if (value instanceof DeepCopyable)
						value = ((DeepCopyable<V>)value).deepCopy();
					tell(dto.shallowCopy(value), GET, dto.source(), message.interaction());
				}
				else if (message.source() == primary) {
					if (message.tag()==SET)
						cache.put(dto.key(), dto.value());
					else if (message.tag()==DEL)
						cache.remove(dto.key());
					else if (message.tag()==DEL_ALL || message.tag()==CLEAR)
						cache.clear();
					else if (message.tag()==EVICT)
						cache.evict(message.valueAsLong());
					else
						unhandled(message);
				}
				else {
					if (message.tag()==SET)
						publish(dto, SET);
					else if (message.tag()==UPDATE)
						; // empty
					else if (message.tag()==DEL)
						publish(dto, DEL);
					else if (message.tag()==DEL_ALL || message.tag()==CLEAR)
						publish(dto, DEL_ALL);
					else {
						unhandled = true;
						unhandled(message);
					}
					
					if (unhandled)
						tell(dto, ActorMessage.UNHANDLED, dto.source(), message.interaction());
				}
			}
			catch(Exception e) {
				e.printStackTrace();
				
				tell(VolatileFailureDTO.of(dto, e), FAILURE, dto.source(), message.interaction());
			}
		}
		else if (message.tag()==EVICT)
			publish(null, EVICT);
		else
			unhandled(message);
	}
}
