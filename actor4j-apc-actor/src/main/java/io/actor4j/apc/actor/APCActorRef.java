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

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import io.actor4j.core.ActorSystem;
import io.actor4j.core.messages.ActorMessage;

public class APCActorRef<I> {
	protected Class<I> interf;
	protected ActorSystem system;
	protected UUID id;

	public APCActorRef(Class<I> interf, ActorSystem system, UUID id) {
		super();
		this.interf = interf;
		this.system = system;
		this.id = id;
	}

	public I tell() {
		@SuppressWarnings("unchecked")
		I result = (I)Proxy.newProxyInstance(interf.getClassLoader(), new java.lang.Class[] { interf },
                new java.lang.reflect.InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						Object result = null;
						
						UUID interaction = null;
						if (method.getReturnType()==Future.class) {
							interaction = UUID.randomUUID();
							result = new CompletableFuture<>();
							((APCActorSystem)system).futureMap.put(interaction, (CompletableFuture<?>)result);
						}
						
						system.send(ActorMessage.create(new APCActorPair(method.getName(), args), 0, system.SYSTEM_ID, id, interaction , null, null));
						
						return result;
					}
                });
		
		return result;
	}

	public UUID getId() {
		return id;
	}
	
	public UUID self() {
		return id;
	}
}
