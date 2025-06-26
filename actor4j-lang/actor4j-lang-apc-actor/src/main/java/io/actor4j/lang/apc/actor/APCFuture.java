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
package io.actor4j.lang.apc.actor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public class APCFuture {
	protected final Map<UUID, CompletableFuture<?>> futureMap = new ConcurrentHashMap<>();
	protected UUID currentFutureId;
	
	protected void setCurrentFutureId(UUID currentFutureId) {
		this.currentFutureId = currentFutureId;
	}

	@SuppressWarnings("unchecked")
	public <T> Future<T> handleFuture(Consumer<CompletableFuture<T>> consumer) {
		CompletableFuture<T> result = (CompletableFuture<T>)futureMap.get(currentFutureId);
		if (result!=null) {
			consumer.accept(result);
			futureMap.remove(currentFutureId);
		}
		
		return result!=null ? (Future<T>)result : null;
	}
}
