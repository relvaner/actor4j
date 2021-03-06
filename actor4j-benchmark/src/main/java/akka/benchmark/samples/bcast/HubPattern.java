/*
 * Copyright (c) 2015-2020, David A. Bauer. All rights reserved.
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
package akka.benchmark.samples.bcast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import akka.actor.ActorRef;
import akka.benchmark.ActorMessage;

public class HubPattern {
	protected List<ActorRef> ports;
	
	protected AtomicLong count;

	public HubPattern() {
		ports = new ArrayList<ActorRef>();
		
		count = new AtomicLong();
	}
	
	public void add(ActorRef ref) {
		ports.add(ref);
	}
	
	public void broadcast(ActorMessage message, ActorRef source) {
		count.getAndIncrement();
		for (ActorRef dest : ports)
			dest.tell(message, source);
	}
	
	public long getCount() {
		return count.get();
	}
}
