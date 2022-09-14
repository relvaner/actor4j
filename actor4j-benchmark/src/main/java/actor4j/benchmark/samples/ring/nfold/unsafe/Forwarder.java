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
package actor4j.benchmark.samples.ring.nfold.unsafe;

import java.util.UUID;

import io.actor4j.core.actors.ActorWithGroup;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.runtime.InternalActorCell;
import io.actor4j.core.utils.ActorGroup;

public class Forwarder extends ActorWithGroup {
	protected UUID next;
	
	protected long initalMessages;
	
	public Forwarder(ActorGroup group) {
		super(group);
	}
	
	public Forwarder(ActorGroup group, UUID next) {
		super(group);
		
		this.next = next;
	}

	@Override
	public void receive(ActorMessage<?> message) {
		if (next!=null)
			((InternalActorCell)cell).unsafe_send(message.shallowCopy(next));
		else 
			((InternalActorCell)cell).unsafe_send(message.shallowCopy(message.valueAsUUID()));
	}
}
