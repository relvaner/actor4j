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
	
	@Override
	public void receive(ActorMessage<?> message) {
		if (message.value()!=null && message.value() instanceof PersistentDataAccessObject) {
			@SuppressWarnings("unchecked")
			PersistentDataAccessObject<K,V> obj = (PersistentDataAccessObject<K,V>)message.value();
			
			if (message.tag()==GET) {
				obj.value = cache.get(obj.key);
				if (obj.value!=null)
					tell(obj, GET, obj.source, message.interaction()); // normally deep copy necessary of obj.value
				else
					tell(message.value(), GET, dataAccess, message.interaction());
			}
			else if (message.tag()==SET) {
				obj.reserved = cache.get(obj.key) != null;
				cache.put(obj.key, obj.value);
				tell(message.value(), SET, dataAccess);
			}
			else if (message.tag()==UPDATE) {
				cache.remove(obj.key);
				tell(message.value(), UPDATE, dataAccess);
			}
			else if (message.tag()==DEL) {
				cache.remove(obj.key);
				tell(message.value(), DELETE_ONE, dataAccess);
			}
			else if (message.tag()==DEL_ALL) {
				cache.clear();
				// drop collection
			}
			else if (message.tag()==CLEAR)
				cache.clear();
			else if (message.source()==dataAccess && message.tag()==FIND_ONE) {
				cache.put(obj.key, obj.value);
				tell(obj, GET, obj.source, message.interaction());
			}
			else if (message.tag()==CAS || message.tag()==CAU) {
				V v = cache.get(obj.key);
				if (v.hashCode()==obj.hashCodeExpected) {
					int tag = SET;
					if (message.tag()==CAU)
						tag = UPDATE;
					receive(message.weakCopy(tag));
				}
				else
					tell(obj, message.tag(), obj.source, message.interaction());
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
