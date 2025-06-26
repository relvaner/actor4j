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
package io.actor4j.lang.apc.channels.runtime;

import java.util.LinkedList;
import java.util.List;

import io.actor4j.core.ActorSystem;
import io.actor4j.core.actors.Actor;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.lang.apc.channels.APC;
import io.actor4j.lang.apc.channels.Callable;
import io.actor4j.lang.apc.channels.Channel;
import static io.actor4j.core.messages.ActorReservedTag.*;

public class APCImpl implements APC {
	protected final int APC_CALL = RESERVED_LANG_APC_CALL;
	
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
	public <T> Channel<T> createChannel() {
		return (Channel<T>)new APCLinkedBlockingQueue<T>();
	}
	
	@Override
	public <T> Channel<T> createChannel(int size) {
		return (Channel<T>)new APCLinkedBlockingQueue<T>(size);
	}
	
	@Override
	public <T> void fork(Callable<T> callable, Channel<T> channel) {
		if (system!=null) {
			ActorId dest = system.addActor(() -> new Actor() {
				@Override
				public void receive(ActorMessage<?> message) {
					if (message.tag()==APC_CALL)
						callable.call(channel);
				}});
			system.send(ActorMessage.create(null, APC_CALL, system.SYSTEM_ID(), dest));
		}
	}
	
	@Override
	public <T> T take(Channel<T> channel) {
		T result = null;
		
		try {
			result = channel.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	@Override
	public <T> List<T> take(Channel<T> channel, int count) {
		List<T> result = new LinkedList<>();
		
		for(int i=0; i<count; i++)
			try {
				result.add(channel.take());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		
		return result;
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
