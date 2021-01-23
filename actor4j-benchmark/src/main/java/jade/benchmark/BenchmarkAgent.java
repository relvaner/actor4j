/*
 * Copyright (c) 2015-2018, David A. Bauer. All rights reserved.
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
package jade.benchmark;

import java.util.concurrent.atomic.AtomicLong;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public abstract class BenchmarkAgent extends Agent {
	protected static final long serialVersionUID = -3608980251344945833L;
	
	public static final AtomicLong counter = new AtomicLong();
	
	public void setup() {
		addBehaviour(new CyclicBehaviour() {
			protected static final long serialVersionUID = -4973585267731749822L;

			public void action() {
				ACLMessage message = receive();
				if (message!=null) {
					counter.getAndIncrement();
					receive(message);
				}
				else
					block();
			}
		});
	}
	
	public abstract void receive(ACLMessage message);
}
