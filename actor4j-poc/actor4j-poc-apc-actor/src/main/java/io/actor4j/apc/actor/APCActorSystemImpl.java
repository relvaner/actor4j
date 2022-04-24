/*
 * Copyright (c) 2016-2019, David A. Bauer. All rights reserved.
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
package io.actor4j.apc.actor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import io.actor4j.core.config.ActorSystemConfig;
import io.actor4j.core.runtime.DefaultActorSystemImpl;

public class APCActorSystemImpl extends DefaultActorSystemImpl implements APCActorSystem {
	protected final Map<UUID, CompletableFuture<?>> futureMap = new ConcurrentHashMap<>();
	
	public APCActorSystemImpl() {
		this(null);
	}
	
	public APCActorSystemImpl(ActorSystemConfig config) {
		super(config);
	}

	@Override
	public <I, T extends I> APCActorRef<I> addAPCActor(Class<I> interf, T obj) {
		UUID id = addActor(() -> new APCActor(interf, obj));
		
		return new APCActorRef<>(interf, this, id);
	}
	
	@SuppressWarnings("unchecked")
	protected <T> Future<T> handleFuture(UUID futureId, Consumer<CompletableFuture<T>> consumer) {
		CompletableFuture<?> result = futureMap.get(futureId);
		if (result!=null) {
			consumer.accept((CompletableFuture<T>)result);
			futureMap.remove(futureId);
		}
		
		return result!=null ? (Future<T>)result : null;
	}
}
