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

import io.actor4j.core.ActorSystem;
import io.actor4j.lang.apc.actor.runtime.APCImpl;

public interface APC {
	public ActorSystem getSystem();
	
	public <I, T extends I> APCActorRef<I> addActor(Class<I> interf, T obj);
	
	public static APC create(ActorSystem system) {
		return new APCImpl(system);
	}
	
	public void start();
	public void shutdown();
	public void shutdown(boolean await);
	public void shutdownWithActors();
	public void shutdownWithActors(final boolean await);
}
