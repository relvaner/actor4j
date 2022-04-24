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
package actor4j.benchmark.samples.ring.nfold.embedded.classic;

import io.actor4j.core.actors.EmbeddedActor;
import io.actor4j.core.actors.EmbeddedHostActor;
import io.actor4j.core.runtime.ActorThread;
import io.actor4j.core.messages.ActorMessage;

public class Forwarder extends EmbeddedActor {
	protected EmbeddedActor next;

	public Forwarder(EmbeddedHostActor host, EmbeddedActor next) {
		super(host);
		
		this.next = next;
	}

	@Override
	public boolean receive(ActorMessage<?> message) {
		((ActorThread)Thread.currentThread()).getCounter().getAndIncrement();  // TODO: for other runtimes
		if (next!=null)
			next.embedded(message);
		else
			return false; // no cycle allowed -> stack overflow
		
		return true;
	}
}
