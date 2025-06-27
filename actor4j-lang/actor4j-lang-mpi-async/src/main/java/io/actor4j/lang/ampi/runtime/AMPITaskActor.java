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

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import io.actor4j.core.actors.Actor;
import io.actor4j.core.actors.ActorDistributedGroupMember;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.lang.ampi.AMPI;
import io.actor4j.lang.ampi.AMPIMessage;
import static io.actor4j.core.messages.ActorReservedTag.*;

public class AMPITaskActor extends Actor implements ActorDistributedGroupMember {
	protected static final UUID groupId = UUID.randomUUID();
	
	protected static final AtomicInteger size = new AtomicInteger(0);
	protected final int rank;
	
	protected final AMPIImpl ampiImpl;
	protected final Consumer<AMPI> ampiTask;
	
	public static final int AMPI_START         = RESERVED_LANG_AMPI_START;
	public static final int AMPI_SEND          = RESERVED_LANG_AMPI_SEND;
	public static final int AMPI_ASEND         = RESERVED_LANG_AMPI_ASEND;
	public static final int AMPI_ASEND_CONFIRM = RESERVED_LANG_AMPI_ASEND_CONFIRM;
	
	public AMPITaskActor(Consumer<AMPI> ampiTask) {
		super("process-actor-"+size.get());
		
		rank = size.getAndIncrement();

		ampiImpl = new AMPIImpl(this, rank);
		this.ampiTask = ampiTask;
	}

	@Override
	public UUID getDistributedGroupId() {
		return groupId;
	}

	@Override
	public void preStart() {
		setAlias("process-actor-"+rank);
	}
	
	@Override
	public void receive(ActorMessage<?> message) {
		if (ampiTask!=null) {
			if (message.tag()==AMPI_START) 
				ampiTask.accept(ampiImpl);
			else if (message.tag()==AMPI_ASEND && message.value()!=null && message.value() instanceof AMPIMessage) {
				AMPIMessage<?> ampiMessage = (AMPIMessage<?>)message.value();
				ampiImpl.asend_confirm(ampiMessage.source());
				ampiImpl.setReceivedAMPIMessage(ampiMessage);
				ampiTask.accept(ampiImpl);
			}
			else if (message.value()!=null && message.value() instanceof AMPIMessage) {
				ampiImpl.setReceivedAMPIMessage((AMPIMessage<?>)message.value());
				ampiTask.accept(ampiImpl);
			}
		}
	}
	
	public void await(int source, int tag) {
		await((msg) -> {
			boolean result = false;
			
			if (msg.value()!=null && msg.value() instanceof AMPIMessage) {
				AMPIMessage<?> ampiMessage = (AMPIMessage<?>)msg.value();
				if (ampiMessage.source()==source && ampiMessage.tag()==tag) {
					result = true;
				}
			}
			
			return result;
		}, (msg) -> {
			unbecome();
			receive(msg);
		});
	}
}
