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
package io.actor4j.lang.ampi;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.function.Function;

import io.actor4j.core.ActorSystem;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorGroup;
import io.actor4j.core.utils.ActorGroupList;
import io.actor4j.lang.ampi.runtime.AMPITaskActor;

public class AMPILauncher {
	protected final ActorSystem system;
	
	public AMPILauncher(ActorSystem system) {
		super();
		this.system = system;
	}

	public ActorSystem getSystem() {
		return system;
	}
	
	public static AMPILauncher create(ActorSystem system) {
		return new AMPILauncher(system);
	}

	public void start(Function<AMPI, Boolean> task, int instances) {
		CountDownLatch countDownLatch = new CountDownLatch(instances);
		Consumer<AMPI> internalAMPITask = (ampi) -> {
			if (task!=null && task.apply(ampi))
				countDownLatch.countDown();
		};
		
		List<ActorId> list = system.addActor(() -> new AMPITaskActor(internalAMPITask), instances);
		ActorGroup group = new ActorGroupList(list);
		system.broadcast(ActorMessage.create(null, AMPITaskActor.AMPI_START, system.SYSTEM_ID(), null), group);
		system.start();
		
		try {
			countDownLatch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		system.shutdownWithActors(true);
	}
}
