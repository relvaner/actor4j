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
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.DeepCopyable;

import static io.actor4j.core.data.access.DataAccessActor.*;

import java.util.UUID;

public class PersistentCacheActor<K, V> extends ActorWithCache<K, V> {
	protected UUID dataAccess;
	
	public PersistentCacheActor(String name, int cacheSize, UUID dataAccess) {
		super(name, cacheSize);
		this.dataAccess = dataAccess;
	}
	
	public PersistentCacheActor(int cacheSize, UUID dataAcess) {
		this(null, cacheSize, dataAcess);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void receive(ActorMessage<?> message) {
		if (message.value()!=null && message.value() instanceof PersistentDataAccessDTO) {
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
			}
			else if (message.tag()==UPDATE) {
				cache.remove(dto.key());
				tell(message.value(), UPDATE, dataAccess);
			}
			else if (message.tag()==DEL) {
				cache.remove(dto.key());
				tell(message.value(), DELETE_ONE, dataAccess);
			}
			else if (message.tag()==DEL_ALL) {
				cache.clear();
				// drop collection
			}
			else if (message.tag()==CLEAR)
				cache.clear();
			else if (message.source()==dataAccess && message.tag()==FIND_ONE) {
				cache.put(dto.key(), dto.value());
				tell(dto, GET, dto.source(), message.interaction());
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
		else if (message.tag()==GC)
			cache.gc(message.valueAsLong());
		else
			unhandled(message);
	}
}
