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
package jade.benchmark.samples.ping.pong;

import java.util.List;

import jade.core.AID;
import jade.core.Agent;

public class Hub extends Agent {
	protected static final long serialVersionUID = 5618388327129574665L;

	protected List<String> ports;
	protected HubPattern hubPattern;

	public Hub() {
		super();
	}
	
	@SuppressWarnings("unchecked")
	public void setup() {
		super.setup();
		
		hubPattern = new HubPattern((List<AID>)getArguments()[0]);
		
		send(hubPattern.broadcast(getAID()));
	}
}
