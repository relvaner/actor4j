/*
 * Copyright (c) 2015-2017, David A. Bauer. All rights reserved.
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
package actor4j.benchmark.samples.ring.nfold.embedded.queue;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import io.actor4j.core.actors.EmbeddedHostActor;
import io.actor4j.core.messages.ActorMessage;

public class Host extends EmbeddedHostActor {
	protected int count;
	protected UUID next;
	
	public static AtomicBoolean stop = new AtomicBoolean(false);
	
	public Host(Integer count) {
		super(false, true);
		
		this.count = count;
	}
	
	@Override
	public void preStart() {
		next = addEmbeddedChild(() -> new Forwarder(null));
		for(int i=0; i<count-2; i++) {
			next = addEmbeddedChild(() -> new Forwarder(next));
		}
	}

	@Override
	public void receive(ActorMessage<?> message) {
		while (!stop.get())
			embedded(ActorMessage.create(next, 0, self(), next));
	}
}
