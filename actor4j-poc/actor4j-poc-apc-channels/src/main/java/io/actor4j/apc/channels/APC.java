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
package io.actor4j.apc.channels;

import java.util.LinkedList;
import java.util.List;

import io.actor4j.apc.channels.runtime.APCLinkedBlockingQueue;
import io.actor4j.core.ActorSystem;
import io.actor4j.core.actors.Actor;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.messages.ActorMessage;

public class APC {
	protected final int APC_CALL = 1010;
	
	protected final ActorSystem system;

	public APC(ActorSystem system) {
		super();
		this.system = system;
	}
	
	public static APC create(ActorSystem system) {
		return new APC(system);
	}

	public <T> Channel<T> createChannel() {
		return (Channel<T>)new APCLinkedBlockingQueue<T>();
	}
	
	public <T> Channel<T> createChannel(int size) {
		return (Channel<T>)new APCLinkedBlockingQueue<T>(size);
	}
	
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
	
	public <T> T take(Channel<T> channel) {
		T result = null;
		
		try {
			result = channel.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
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
	
	public void start() {
		if (system!=null)
			system.start();
	}
	
	public void shutdown() {
		if (system!=null)
			system.shutdown();
	}
	
	public void shutdown(boolean await) {
		if (system!=null)
			system.shutdown(await);
	}
}
