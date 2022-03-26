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
package io.actor4j.examples.stateless;

import java.util.Timer;
import java.util.TimerTask;

import io.actor4j.core.ActorSystem;
import io.actor4j.core.actors.ActorWithDistributedGroup;
import io.actor4j.core.config.ActorSystemConfig;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorGroup;
import io.actor4j.core.utils.ActorGroupSet;

public class ExampleStateless {
	public ExampleStateless() {
		ActorSystemConfig config = ActorSystemConfig.builder()
			.parallelism(2)
			.build();
		ActorSystem system = ActorSystem.create(config);
		
		ActorGroup group = new ActorGroupSet();
		system.setAlias(system.addActor(() -> new ActorWithDistributedGroup(group) {
			@Override
			public void receive(ActorMessage<?> message) {
				System.out.printf("from thread %s of actor %s%n", Thread.currentThread().getName(), self());
			}
		}, system.getConfig().parallelism()*system.getConfig().parallelismFactor()), "instances");
		
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				system.sendViaAlias(ActorMessage.create(null, 0, system.SYSTEM_ID(), null), "instances");
			}
		}, 0, 50);
		
		system.start();
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		timer.cancel();
		system.shutdownWithActors(true);
	}
	
	public static void main(String[] args) {
		new ExampleStateless();
	}
}
