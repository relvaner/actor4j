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

public class VolatileCacheActor<K, V> extends ActorWithCache<K, V> {
	public VolatileCacheActor(String name, int cacheSize) {
		super(name, cacheSize);
	}
	
	public VolatileCacheActor(int cacheSize) {
		this(null, cacheSize);
	}
	
	@Override
	public void receive(ActorMessage<?> message) {
		if (message.value!=null && message.value instanceof  VolatileDataAccessObject) {
			@SuppressWarnings("unchecked")
			 VolatileDataAccessObject<K,V> obj = ( VolatileDataAccessObject<K,V>)message.value;
			
			if (message.tag==GET) {
				obj.value = cache.get(obj.key);
				tell(obj, GET, obj.source, message.interaction); // normally deep copy necessary of obj.value
			}
			else if (message.tag==SET)
				cache.put(obj.key, obj.value);
			else if (message.tag==UPDATE)
				; // empty
			else if (message.tag==DEL)
				cache.remove(obj.key);
			else if (message.tag==DEL_ALL || message.tag==CLEAR)
				cache.clear();
			else
				unhandled(message);
		}
		else if (message.tag==GC)
			cache.gc(message.valueAsLong());
		else
			unhandled(message);
	}
}
