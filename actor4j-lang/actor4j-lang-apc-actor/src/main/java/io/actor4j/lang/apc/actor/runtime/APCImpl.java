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
package io.actor4j.lang.apc.actor.runtime;

import io.actor4j.core.ActorSystem;
import io.actor4j.core.id.ActorId;
import io.actor4j.lang.apc.actor.APC;
import io.actor4j.lang.apc.actor.APCActor;
import io.actor4j.lang.apc.actor.APCActorRef;

public class APCImpl implements APC {
	protected final ActorSystem system;
	
	public APCImpl(ActorSystem system) {
		super();
		this.system = system;
	}
	
	@Override
	public ActorSystem getSystem() {
		return system;
	}

	@Override
	public <I, T extends I> APCActorRef<I> addActor(Class<I> interf, T obj) {
		ActorId id = system.addActor(() -> new APCActor(interf, obj, this));
		
		return new APCActorRef<>(interf, obj, this, id);
	}
	
	@Override
	public void start() {
		if (system!=null)
			system.start();
	}
	
	@Override
	public void shutdown() {
		if (system!=null)
			system.shutdown();
	}
	
	@Override
	public void shutdown(boolean await) {
		if (system!=null)
			system.shutdown(await);
	}
	
	@Override
	public void shutdownWithActors() {
		if (system!=null)
			system.shutdownWithActors();
	}
	
	@Override
	public void shutdownWithActors(final boolean await) {
		if (system!=null)
			system.shutdownWithActors(await);
	}
}
