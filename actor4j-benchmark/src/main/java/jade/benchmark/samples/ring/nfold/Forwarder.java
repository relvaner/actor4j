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
package jade.benchmark.samples.ring.nfold;

import jade.benchmark.BenchmarkAgent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;

public class Forwarder extends BenchmarkAgent {
	protected static final long serialVersionUID = -7845503385833074838L;
	
	protected AID next;

	public Forwarder() {
		super();
	}

	public void setup() {
		super.setup(); 
		String localName = getArguments()!=null ? (String)getArguments()[0] : null;
		if (localName!=null)
			this.next = new AID(localName, AID.ISLOCALNAME);
	}
	
	@Override
	public void receive(ACLMessage message) {
		if (next!=null) {
			ACLMessage response = new ACLMessage(ACLMessage.INFORM);
			response.addReceiver(next);
			response.setContent(message.getContent());
			send(response);
		}
		else {
			ACLMessage response = new ACLMessage(ACLMessage.INFORM);
			response.addReceiver(new AID(message.getContent(), AID.ISLOCALNAME));
			response.setContent(message.getContent());
			send(response);
		}
	}
}
