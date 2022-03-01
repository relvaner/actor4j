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
package io.actor4j.examples.verification;

import static io.actor4j.core.logging.ActorLogger.*;

import java.util.LinkedList;
import java.util.List;

import io.actor4j.core.actors.Actor;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.verification.ActorVerification;
import io.actor4j.verification.ActorVerificationSM;
import io.actor4j.verification.ActorVerificationUtils;
import io.actor4j.verification.ActorVerificator;

public class ExampleVerification {
	public static final int PING = 100;
	public static final int PONG = 101;
	
	protected static class Ping extends Actor implements ActorVerification {
		public Ping(String name) {
			super(name);
		}
		
		@Override
		public void receive(ActorMessage<?> message) {
			tell("ping", PING, message.source());
		}
		
		@Override
		public ActorVerificationSM verify() {
			ActorVerificationSM result = new ActorVerificationSM(this);
			result
				.addInitialStateMarker("PING")
				.addInTransition("PING", "PING", PONG)
				.addOutTransition("PING", "PING", PING, "pong")
			
				.addStateMarker("D")
				.addInTransition("PING", "D", 0)
			
				.addStateMarker("A")
				.addStateMarker("B")
				.addStateMarker("C")
				.addInTransition("A", "B", 0)
				.addInTransition("B", "C", 0)
				.addInTransition("C", "A", 0);
			
			return result;
		}
	}
	
	protected static class Pong extends Actor implements ActorVerification {
		public Pong(String name) {
			super(name);
		}
		
		@Override
		public void receive(ActorMessage<?> message) {
			tell("pong", PONG, message.source());
		}
		
		@Override
		public ActorVerificationSM verify() {
			ActorVerificationSM result = new ActorVerificationSM(this);
			result
				.addInitialStateMarker("PONG")
				.addInTransition("PONG", "PONG", PING)
				.addOutTransition("PONG", "PONG", PONG, "ping");
			
			return result;
		}
	}

	public static void main(String[] args) {
		ActorVerificator verificator = new ActorVerificator();
		
		verificator.addActor(() -> new Ping("ping"));
		verificator.addActor(() -> new Pong("pong"));
		
		String globalIntialStateMarker = "ping:PING";
		
		List<ActorVerificationSM> list = new LinkedList<>();
		
		verificator.verifyAll((sm) -> {
			list.add(sm);
			logger().log(DEBUG, String.format("%s - Cycles: %s", sm.getName(), ActorVerificationUtils.findCycles(sm.getGraph())));
			logger().log(DEBUG, String.format("%s - Unreachables: %s", sm.getName(), ActorVerificationUtils.findUnreachables(sm.getGraph(), sm.getIntialStateMarker())));
			logger().log(DEBUG, String.format("%s - Deads: %s", sm.getName(), ActorVerificationUtils.findDead(
					sm.getGraph(), ActorVerificationUtils.findUnreachables(sm.getGraph(), sm.getIntialStateMarker()))));
			
			logger().log(DEBUG, String.format("%s - Edges (initial state, self reference): %s", sm.getName(), sm.getGraph().getAllEdges(sm.getIntialStateMarker(), sm.getIntialStateMarker())));
		}, (graph) -> {
			ActorVerificationUtils.interconnect(list, graph);
			logger().log(DEBUG, String.format("All - Cycles: %s", ActorVerificationUtils.findCycles(graph)));
			logger().log(DEBUG, String.format("All - Unreachables: %s", ActorVerificationUtils.findUnreachables(graph, globalIntialStateMarker)));
			logger().log(DEBUG, String.format("All - Deads: %s", ActorVerificationUtils.findDead(
					graph, ActorVerificationUtils.findUnreachables(graph, globalIntialStateMarker))));
		});
	}
}
