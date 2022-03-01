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
package actor4j.benchmark.samples.ping.pong.grouped.bulk;

import static actor4j.benchmark.samples.ping.pong.grouped.bulk.ActorMessageTag.*;

import java.util.UUID;

import io.actor4j.core.actors.ActorWithGroup;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorGroup;

public class Client extends ActorWithGroup {
	protected UUID dest;
	
	protected long initalMessages;
	
	public Client(ActorGroup group, UUID dest) {
		super(group);
		
		this.dest = dest;
		
		initalMessages = 100;
	}

	@Override
	public void receive(ActorMessage<?> message) {
		if (message.tag()==MSG.ordinal())
			send(message.shallowCopy(self(), message.source()));
		else if (message.tag()==RUN.ordinal())
            for (int i=0; i<initalMessages; i++) {
            	send(ActorMessage.create(null, MSG, self(), dest));
		}
	}
}
