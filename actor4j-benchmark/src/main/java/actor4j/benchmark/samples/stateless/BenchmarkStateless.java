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
package actor4j.benchmark.samples.stateless;

import java.util.LinkedList;
import java.util.List;

import actor4j.benchmark.Benchmark;
import io.actor4j.core.actors.ActorWithDistributedGroup;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorGroup;
import io.actor4j.core.utils.ActorGroupSet;
import io.actor4j.corex.XActorSystem;
import shared.benchmark.BenchmarkConfig;

public class BenchmarkStateless {
	public BenchmarkStateless(BenchmarkConfig config) {
		super();
		
		XActorSystem system = new XActorSystem("actor4j::Stateless");
		system.parkMode();
		
		System.out.printf("#actors: %d%n", config.parallelism());
		ActorGroup group = new ActorGroupSet();
		system.setAlias(system.addActor(() -> new ActorWithDistributedGroup(group) {
			@Override
			public void receive(ActorMessage<?> message) {
			}
		}, config.parallelism()), "instances");
		
		
		List<Thread> threads = new LinkedList<>();
		for (int i=0; i<system.getParallelismMin()*system.getParallelismFactor(); i++)
			threads.add(new Thread(new Runnable() {
				@Override
				public void run() {
					while (!Thread.currentThread().isInterrupted())
						system.sendViaAlias(new ActorMessage<Object>(new Object(), 0, system.SYSTEM_ID, null), "instances");
				}
			}));
		for (Thread t : threads)
			t.start();
		
		Benchmark benchmark = new Benchmark(system, config);
		benchmark.start();
		
		for (Thread t : threads)
			t.interrupt();
	}
	
	public static void main(String[] args) {
		new BenchmarkStateless(new BenchmarkConfig());
	}
}
