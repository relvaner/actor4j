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
import io.actor4j.core.utils.CacheLRUWithGC;

public class SecondaryVolatileCacheActor<K, V> extends SecondaryActor {
	protected int cacheSize;
	protected Cache<K, V> cache;

	public SecondaryVolatileCacheActor(ActorGroup group, UUID primary, int cacheSize) {
		this(null, group, primary, cacheSize);
	}

	public SecondaryVolatileCacheActor(String name, ActorGroup group, UUID primary, int cacheSize) {
		super(name, group, primary);
		
		this.cacheSize = cacheSize;
		cache = new CacheLRUWithGC<>(cacheSize);
	}

	@Override
	public void receive(ActorMessage<?> message) {
		if (message.value()!=null && message.value() instanceof VolatileDataAccessObject) {
			@SuppressWarnings("unchecked")
			VolatileDataAccessObject<K,V> dto = (VolatileDataAccessObject<K,V>)message.value();
			
			if (message.tag()==GET) {
				V value = cache.get(dto.key());
				tell(dto.shallowCopy(value), GET, dto.source(), message.interaction()); // normally deep copy necessary of dto.value()
			}
			else if (message.source() == primary) {
				if (message.tag()==SET)
					cache.put(dto.key(), dto.value());
				else if (message.tag()==DEL)
					cache.remove(dto.key());
				else if (message.tag()==DEL_ALL || message.tag()==CLEAR)
					cache.clear();
				else if (message.tag()==GC)
					cache.gc(message.valueAsLong());
				else
					unhandled(message);
			}
			else {
				if (message.tag()==SET)
					publish(VolatileDTO.create(dto.key(), dto.value()), SET);
				else if (message.tag()==UPDATE)
					; // empty
				else if (message.tag()==DEL)
					publish(VolatileDTO.create(dto.key()), DEL);
				else if (message.tag()==DEL_ALL || message.tag()==CLEAR)
					publish(VolatileDTO.create(), DEL_ALL);
				else
					unhandled(message);
			}
		}
		else if (message.tag()==GC)
			publish(null, GC);
		else
			unhandled(message);
	}
}
