/*
 * Copyright (c) 2015-2024, David A. Bauer. All rights reserved.
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
package io.actor4j.lang.ampi.runtime;

import io.actor4j.core.actors.ActorRef;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.lang.ampi.AMPI;
import io.actor4j.lang.ampi.AMPIMessage;

public class AMPIImpl implements AMPI {
	protected final ActorRef actorRef;
	protected final int rank;
	
	protected AMPIMessage<?> receivedAMPIMessage;
	
	public AMPIImpl(ActorRef actorRef, int rank) {
		this.actorRef = actorRef;
		this.rank = rank;
	}

	@Override
	public int rank() {
		return rank;
	}

	@Override
	public int size() {
		return AMPITaskActor.size.get();
	}

	@Override
	public <T> void send(T t, int dest, int tag) {
		actorRef.sendViaAlias(ActorMessage.create(new AMPIMessage<T>(t, tag, rank), AMPITaskActor.AMPI_SEND, actorRef.self(), null), getAlias(dest));
	}
	
	protected void setReceivedAMPIMessage(AMPIMessage<?> message) {
		receivedAMPIMessage = message;
	}
	
	@Override
	public boolean probe(int source, int tag) {
		return receivedAMPIMessage!=null && receivedAMPIMessage.source()==source && receivedAMPIMessage.tag()==tag;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T receive(int source, int tag) {
		T result = null;
		
		if (receivedAMPIMessage!=null && receivedAMPIMessage.source()==source && receivedAMPIMessage.tag()==tag) {
			result = (T)receivedAMPIMessage.value();
			receivedAMPIMessage = null;
		}
		
		return result;
	}
	
	public static String getAlias(int rank) {
		return "process-actor-"+rank;
	}

	@Override
	public <T> void asend(T t, int dest, int tag) {
		actorRef.sendViaAlias(ActorMessage.create(new AMPIMessage<T>(t, tag, rank), AMPITaskActor.AMPI_ASEND, actorRef.self(), null), getAlias(dest));
		await(dest, AMPITaskActor.AMPI_ASEND_CONFIRM);
	}
	
	public void asend_confirm(int dest) {
		actorRef.sendViaAlias(ActorMessage.create(null, AMPITaskActor.AMPI_ASEND_CONFIRM, actorRef.self(), null), getAlias(dest));
	}
	
	@Override
	public void await(int source, int tag) {
		((AMPITaskActor)actorRef).await(source, tag);
	}
}
