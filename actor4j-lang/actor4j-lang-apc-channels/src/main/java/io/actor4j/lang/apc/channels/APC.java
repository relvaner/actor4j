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
package io.actor4j.lang.apc.channels;

import java.util.List;

import io.actor4j.core.ActorSystem;
import io.actor4j.lang.apc.channels.runtime.APCImpl;

public interface APC {
	public ActorSystem getSystem();
	
	public static APC create(ActorSystem system) {
		return new APCImpl(system);
	}

	public <T> Channel<T> createChannel();
	public <T> Channel<T> createChannel(int size);
	
	public <T> void fork(Callable<T> callable, Channel<T> channel);
	
	public <T> T take(Channel<T> channel);
	public <T> List<T> take(Channel<T> channel, int count);
	
	public void start();
	public void shutdown(); 
	public void shutdown(boolean await);
	public void shutdownWithActors();
	public void shutdownWithActors(final boolean await);
}
