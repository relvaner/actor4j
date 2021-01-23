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
package jade.benchmark.samples.ping.pong.bulk;

import java.util.ArrayList;
import java.util.List;

import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;

public class HubPattern {
	protected List<AID> ports;

	public HubPattern() {
		ports = new ArrayList<AID>();
	}
	
	public HubPattern(List<AID> ports) {
		this.ports = ports;
	}
	
	public void add(AID ref) {
		ports.add(ref);
	}
	
	public ACLMessage broadcast(AID source) {
		ACLMessage result = new ACLMessage(ACLMessage.INFORM);
		
		result.setSender(source);
		for (AID dest : ports)
			result.addReceiver(dest);
		
		return result;
	}
	
	public void broadcast(Agent agent, long initalMessages) {
		for (AID dest : ports)
			for (int i=0; i<initalMessages; i++) {
				ACLMessage message = new ACLMessage(ACLMessage.INFORM);
				message.setSender(agent.getAID());
				message.addReceiver(dest);
				agent.send(message);
			}
	}
}
