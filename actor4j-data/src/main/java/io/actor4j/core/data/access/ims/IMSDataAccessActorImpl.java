/*
 * Copyright (c) 2015-2024, David A. Bauer. All rights reserved.
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
package io.actor4j.core.data.access.ims;

import static io.actor4j.core.actors.ActorWithCache.*;
import static io.actor4j.core.data.access.DataAccessActor.*;

import io.actor4j.core.actors.ActorRef;
import io.actor4j.core.data.access.BaseDataAccessActorImpl;
import io.actor4j.core.data.access.DataAccessActor;
import io.actor4j.core.data.access.PersistentDataAccessDTO;
import io.actor4j.core.data.access.PersistentFailureDTO;
import io.actor4j.core.data.access.PersistentSuccessDTO;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.DeepCopyable;

public class IMSDataAccessActorImpl<K, V> extends BaseDataAccessActorImpl<K, V>{
	protected IMS<K, V> ims;
	
	public IMSDataAccessActorImpl(ActorRef dataAccess) {
		super(dataAccess);
		
		ims = new IMS<>();
	}

	@Override
	public void onReceiveMessage(ActorMessage<?> msg, PersistentDataAccessDTO<K, V> dto) {
		// empty
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onFindOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, V> dto) {
		if (dto.key()!=null) {
			V value = ims.getData().get(dto.key());
			if (value==null && !ims.getData().containsKey(dto.key()))
				dataAccess.tell(dto, FIND_NONE, msg.source(), msg.interaction());
			else {
				if (value!=null) {
					if (value instanceof DeepCopyable)
						value = ((DeepCopyable<V>)value).deepCopy();
					dataAccess.tell(dto.shallowCopy(value), FIND_ONE, msg.source(), msg.interaction());
				}
				else
					dataAccess.tell(dto, FIND_ONE, msg.source(), msg.interaction());
			}
		}
		else
			dataAccess.tell(dto, FIND_NONE, msg.source(), msg.interaction());
	}

	@Override
	public boolean hasOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, V> dto) {
		boolean result = false;
		
		if (dto.key()!=null)
			result = ims.getData().containsKey(dto.key());
		
		return result;
	}

	@Override
	public void insertOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, V> dto) {
		updateOne(msg, dto);
	}

	@Override
	public void replaceOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, V> dto) {
		updateOne(msg, dto);
	}

	@Override
	public void updateOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, V> dto) {
		 ims.put(dto.key(), dto.value());
	}

	@Override
	public void deleteOne(ActorMessage<?> msg, PersistentDataAccessDTO<K, V> dto) {
		if (dto.key()!=null)
			ims.remove(dto.key());
	}

	@Override
	public boolean handleMessage(ActorMessage<?> msg, PersistentDataAccessDTO<K, V> dto) {
		return false;
	}

	@Override
	public void onSuccess(ActorMessage<?> msg, PersistentDataAccessDTO<K, V> dto) {
		if (msg.tag()!=FIND_ONE && msg.tag()!=GET && msg.tag()!=HAS_ONE)
			dataAccess.tell(PersistentSuccessDTO.of(dto, msg.tag()), DataAccessActor.SUCCESS, msg.source(), msg.interaction());
	}

	@Override
	public void onFailure(ActorMessage<?> msg, PersistentDataAccessDTO<K, V> dto, Throwable t) {
		dataAccess.tell(PersistentFailureDTO.of(dto, msg.tag(), t), DataAccessActor.FAILURE, msg.source(), msg.interaction());
	}
}
