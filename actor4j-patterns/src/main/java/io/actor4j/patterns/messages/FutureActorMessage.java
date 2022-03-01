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
package io.actor4j.patterns.messages;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.messages.ActorMessageUtils;
import io.actor4j.core.utils.DeepCopyable;
import io.actor4j.core.utils.Shareable;

public record FutureActorMessage<T>(CompletableFuture<T> future, T value, int tag, UUID source, UUID dest, UUID interaction, String protocol, String domain) implements ActorMessage<T> {
	public FutureActorMessage(CompletableFuture<T> future, T value, int tag, UUID source, UUID dest) {
		this(future, value, tag, source, dest, null, null, null);
	}

	public FutureActorMessage(CompletableFuture<T> future, T value, Enum<?> tag, UUID source, UUID dest) {
		this(future, value, tag.ordinal(), source, dest);
	}
	
	@Override
	public ActorMessage<T> shallowCopy() {
		return new FutureActorMessage<T>(future, value, tag, source, dest, interaction, protocol, domain);
	}
	
	@Override
	public ActorMessage<T> shallowCopy(T value) {
		return !this.value.equals(value) ? new FutureActorMessage<T>(future, value, tag, source, dest, interaction, protocol, domain) : this;
	}
	
	@Override
	public ActorMessage<T> shallowCopy(int tag) {
		return this.tag!=tag ? new FutureActorMessage<T>(future, value, tag, source, dest, interaction, protocol, domain) : this;
	}
	
	@Override
	public ActorMessage<T> shallowCopy(T value, int tag) {
		return !this.value.equals(value) || this.tag!=tag ? new FutureActorMessage<T>(future, value, tag, source, dest, interaction, protocol, domain) : this;
	}
	
	@Override
	public ActorMessage<T> shallowCopy(int tag, String protocol) {
		return this.tag!=tag || !this.protocol.equals(protocol) ? new FutureActorMessage<T>(future, value, tag, source, dest, interaction, protocol, domain) : this;
	}
	
	@Override
	public ActorMessage<T> shallowCopy(UUID source, UUID dest) {
		return !this.source.equals(source) || !this.dest.equals(dest) ? new FutureActorMessage<T>(future, value, tag, source, dest, interaction, protocol, domain) : this;
	}
	
	@Override
	public ActorMessage<T> shallowCopy(UUID dest) {
		return !this.dest.equals(dest) ? new FutureActorMessage<T>(future, value, tag, source, dest, interaction, protocol, domain) : this;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ActorMessage<T> copy() {
		if (value!=null) { 
			if (ActorMessageUtils.isSupportedType(value.getClass()) || value instanceof Shareable)
				return this;
			else if (value instanceof DeepCopyable)
				return new FutureActorMessage<T>(future, ((DeepCopyable<T>)value).deepCopy(), tag, source, dest, interaction, protocol, domain);
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
			if (ActorMessageUtils.isSupportedType(value.getClass()) || value instanceof Shareable)
				return !this.dest.equals(dest) ? new FutureActorMessage<T>(future,value, tag, source, dest, interaction, protocol, domain) : this;
			else if (value instanceof DeepCopyable)
				return new FutureActorMessage<T>(future,((DeepCopyable<T>)value).deepCopy(), tag, source, dest, interaction, protocol, domain);
			else if (value instanceof Exception)
				return !this.dest.equals(dest) ? new FutureActorMessage<T>(future,value, tag, source, dest, interaction, protocol, domain) : this;
			else
				throw new IllegalArgumentException(value.getClass().getName());
		}
		else
			return !this.dest.equals(dest) ? new FutureActorMessage<T>(future,null, tag, source, dest, interaction, protocol, domain) : this;
	}

	@Override
	public String toString() {
		return "FutureActorMessage [value=" + value + ", tag=" + tag + ", source=" + source + ", dest=" + dest
				+ ", interaction=" + interaction + ", protocol=" + protocol + ", domain=" + domain + "]";
	}
}
