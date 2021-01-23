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

import io.actor4j.core.actors.SecondaryActor;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorGroup;
import io.actor4j.core.utils.Cache;
import io.actor4j.core.utils.CacheLRUWithGC;

import java.util.UUID;

import static io.actor4j.core.actors.ActorWithCache.*;

public class SecondaryPersistentCacheActor<K, V> extends SecondaryActor {
	protected int cacheSize;
	protected Cache<K, V> cache;
	
	public SecondaryPersistentCacheActor(ActorGroup group, UUID primary, int cacheSize) {
		this(null, group, primary, cacheSize);
	}

	public SecondaryPersistentCacheActor(String name, ActorGroup group, UUID primary, int cacheSize) {
		super(name, group, primary);
		
		this.cacheSize = cacheSize;
		cache = new CacheLRUWithGC<>(cacheSize);
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
						publish(message);
				}
				else if (message.tag==SET || 
						 message.tag==UPDATE ||
					     message.tag==DEL ||
					     message.tag==DEL_ALL || 
					     message.tag==CLEAR ||
					     message.tag==CAS || 
					     message.tag==CAU || 
					     message.tag==GC)
					publish(message);
				else
					unhandled(message);
			}
			else if (message.value instanceof VolatileDataAccessObject && message.source == primary) {
				@SuppressWarnings("unchecked")
				VolatileDataAccessObject<K,V> obj = (VolatileDataAccessObject<K,V>)message.value;
				
				if (message.tag==SET)
					cache.put(obj.key, obj.value);
				else if (message.tag==DEL)
					cache.remove(obj.key);
				else if (message.tag==DEL_ALL || message.tag==CLEAR)
					cache.clear();
				else
					unhandled(message);
			}
		}
		else if (message.tag==GC)
			cache.gc(message.valueAsLong());
		else
			unhandled(message);
	}
}
