/*
 * Copyright (c) 2015-2018, David A. Bauer. All rights reserved.
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
package io.actor4j.core.data.access.imdb;

import io.actor4j.core.actors.Actor;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.DeepCopyable;
import io.actor4j.core.data.access.PersistentDataAccessObject;

import static io.actor4j.core.actors.ActorWithCache.*;
import static io.actor4j.core.data.access.DataAccessActor.*;
import static io.actor4j.core.data.access.imdb.IMDBUtils.*;

public class IMDBDataAccessActor<K, V> extends Actor {
	protected IMDB<K, V> imdb;
	
	public IMDBDataAccessActor(String name) {
		super(name);
		
		imdb = new IMDB<>();
	}
	
	public IMDBDataAccessActor() {
		this(null);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void receive(ActorMessage<?> message) {
		if (message.value()!=null && message.value() instanceof PersistentDataAccessObject) {
			PersistentDataAccessObject<K,V> dto = (PersistentDataAccessObject<K,V>)message.value();
			
			if (message.tag()==GET) {
				V value = findOne(dto.key(), dto.filter(), imdb, dto.collectionName());
				if (value instanceof DeepCopyable)
					value = ((DeepCopyable<V>)value).deepCopy();
				tell(dto.shallowCopy(value), FIND_ONE, message.source(), message.interaction());
			}
			else if (message.tag()==SET) {
				if (dto.key()!=null) 
					put(dto.key(), dto.value(), imdb, dto.collectionName());
				else {
					if (!((boolean)dto.reserved()) && !hasOne(dto.key(), dto.filter(), imdb, dto.collectionName()))
						insertOne(dto.key(), dto.value(), imdb, dto.collectionName());
					else
						replaceOne(dto.key(), dto.filter(), dto.value(), imdb, dto.collectionName());
				}
			}
			else if (message.tag()==UPDATE_ONE || message.tag()==UPDATE)
				; // empty
			else if (message.tag()==INSERT_ONE) {
				if (dto.filter()!=null) {
					if (!hasOne(dto.key(), dto.filter(), imdb, dto.collectionName()))
						insertOne(dto.key(), dto.value(), imdb, dto.collectionName());
				}
				else
					insertOne(dto.key(), dto.value(), imdb, dto.collectionName());
			}
			else if (message.tag()==DELETE_ONE)
				deleteOne(dto.key(), dto.filter(), imdb, dto.collectionName());
			else if (message.tag()==HAS_ONE) {
				Object reserved = hasOne(dto.key(), dto.filter(), imdb, dto.collectionName());
				tell(dto.shallowCopyWithReserved(reserved), FIND_ONE, message.source(), message.interaction());
			}
			else
				unhandled(message);
		}
		else
			unhandled(message);
	}
}
