/*
 * Copyright (c) 2015-2022, David A. Bauer. All rights reserved.
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
package io.actor4j.web.utils;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.messages.ActorMessageUtils;
import io.actor4j.core.utils.DeepCopyable;
import io.actor4j.core.utils.Shareable;

public record RemoteActorMessage<T>(T value, int tag, UUID source, UUID dest, UUID interaction, String protocol, String domain) implements ActorMessage<T> {
	public RemoteActorMessage {
		// empty
	}

	public RemoteActorMessage(T value, int tag, UUID source, UUID dest) {
		this(value, tag, source, dest, null, null, null);
	}
	
	public RemoteActorMessage(T value, int tag, UUID source, UUID dest, String domain) {
		this(value, tag, source, dest, null, null, domain);
	}
	
	public RemoteActorMessage(T value, int tag, UUID source, UUID dest, UUID interaction) {
		this(value, tag, source, dest, interaction, null, null);
	}
	
	public RemoteActorMessage(T value, int tag, UUID source, UUID dest, UUID interaction, String protocol) {
		this(value, tag, source, dest, interaction, protocol, null);
	}

	public RemoteActorMessage(T value, Enum<?> tag, UUID source, UUID dest) {
		this(value, tag.ordinal(), source, dest);
	}
	
	public RemoteActorMessage(T value, Enum<?> tag, UUID source, UUID dest, String domain) {
		this(value, tag.ordinal(), source, dest, domain);
	}
	
	public RemoteActorMessage(T value, Enum<?> tag, UUID source, UUID dest, UUID interaction) {
		this(value, tag.ordinal(), source, dest, interaction);
	}
	
	public RemoteActorMessage(T value, Enum<?> tag, UUID source, UUID dest, UUID interaction, String protocol) {
		this(value, tag.ordinal(), source, dest, interaction, protocol);
	}
	
	public RemoteActorMessage(T value, Enum<?> tag, UUID source, UUID dest, UUID interaction, String protocol, String domain) {
		this(value, tag.ordinal(), source, dest, interaction, protocol, domain);
	}

	@SuppressWarnings("unchecked")
	public <C> C convertValue(Class<T> clazz) {
		return (C)(new ObjectMapper().convertValue(value, clazz));
	}

	public boolean valueIsPrimitiveType() {
		return Utils.isWrapperType(value.getClass());
	}
	
	@SuppressWarnings("unchecked")
	public static <C> C convertValue(ActorMessage<?> message, Class<C> clazz) {
		C result = null;
		if ((message instanceof RemoteActorMessage))
			result = ((RemoteActorMessage<C>)message).convertValue(clazz);
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public static <C> C optionalConvertValue(ActorMessage<?> message, Class<C> clazz) {
		C result = null;
		if ((message instanceof RemoteActorMessage) && !((RemoteActorMessage<?>)message).valueIsPrimitiveType())
			result = ((RemoteActorMessage<C>)message).convertValue(clazz);
		return result;
	}
	
	@Override
	public ActorMessage<T> shallowCopy() {
		return new RemoteActorMessage<>(value, tag, source, dest, interaction, protocol, domain);
	}
	
	@Override
	public ActorMessage<T> shallowCopy(T value) {
		return !ActorMessageUtils.equals(this.value, value) ? 
			new RemoteActorMessage<T>(value, tag, source, dest, interaction, protocol, domain) : this;
	}
	
	@Override
	public ActorMessage<T> shallowCopy(int tag) {
		return this.tag!=tag ? 
			new RemoteActorMessage<>(value, tag, source, dest, interaction, protocol, domain) : this;
	}
	
	@Override
	public ActorMessage<T> shallowCopy(T value, int tag) {
		return !ActorMessageUtils.equals(this.value, value) || this.tag!=tag ? 
			new RemoteActorMessage<T>(value, tag, source, dest, interaction, protocol, domain) : this;
	}
	
	@Override
	public ActorMessage<T> shallowCopy(int tag, String protocol) {
		return this.tag!=tag || !ActorMessageUtils.equals(this.protocol, protocol) ? 
			new RemoteActorMessage<T>(value, tag, source, dest, interaction, protocol, domain) : this;
	}
	
	@Override
	public ActorMessage<T> shallowCopy(UUID source, UUID dest) {
		return !ActorMessageUtils.equals(this.source, source) || !ActorMessageUtils.equals(this.dest, dest) ? 
			new RemoteActorMessage<>(value, tag, source, dest, interaction, protocol, domain) : this;
	}
	
	@Override
	public ActorMessage<T> shallowCopy(UUID dest) {
		return !ActorMessageUtils.equals(this.dest, dest) ? 
			new RemoteActorMessage<>(value, tag, source, dest, interaction, protocol, domain) : this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public ActorMessage<T> copy() {
		if (value!=null) { 
			if (ActorMessageUtils.isSupportedType(value.getClass()) || value instanceof Record || value instanceof Shareable)
				return this;
			else if (value instanceof DeepCopyable)
				return new RemoteActorMessage<>(((DeepCopyable<T>)value).deepCopy(), tag, source, dest, interaction, protocol, domain);
			else if (value instanceof Exception)
				return this;
			else
				throw new IllegalArgumentException(value.getClass().getName());
		}
		else
			return this;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ActorMessage<T> copy(UUID dest) {
		if (value!=null) { 
			if (ActorMessageUtils.isSupportedType(value.getClass()) || value instanceof Record || value instanceof Shareable)
				return !ActorMessageUtils.equals(this.dest, dest) ? new RemoteActorMessage<>(value, tag, source, dest, interaction, protocol, domain) : this;
			else if (value instanceof DeepCopyable)
				return new RemoteActorMessage<>(((DeepCopyable<T>)value).deepCopy(), tag, source, dest, interaction, protocol, domain);
			else if (value instanceof Exception)
				return !ActorMessageUtils.equals(this.dest, dest) ? new RemoteActorMessage<>(value, tag, source, dest, interaction, protocol, domain) : this;
			else
				throw new IllegalArgumentException(value.getClass().getName());
		}
		else
			return !ActorMessageUtils.equals(this.dest, dest) ? new RemoteActorMessage<>(null, tag, source, dest, interaction, protocol, domain) : this;
	}
}
