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
package akka.benchmark.samples.ring;

import akka.actor.ActorRef;
import akka.actor.UntypedAbstractActor;
import akka.benchmark.ActorMessage;

public class Forwarder extends UntypedAbstractActor {
    protected ActorRef next;
    
    public Forwarder() {
    	this(null);
    }
    
    public Forwarder(ActorRef next) {
    	super();
    	this.next = next;
    }
	
	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof ActorMessage) {
			if (next!=null)
				next.tell(message, getSelf());
			else
				((ActorRef)((ActorMessage) message).value).tell(message, getSelf());	
		}
	}
}
