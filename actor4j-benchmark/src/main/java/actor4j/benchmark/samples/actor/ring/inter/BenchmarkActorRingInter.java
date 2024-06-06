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
package actor4j.benchmark.samples.actor.ring.inter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import actor4j.benchmark.BenchmarkSampleActor4j;
import io.actor4j.core.ActorSystem;
import io.actor4j.core.actors.Actor;
import io.actor4j.core.actors.PseudoActor;
import io.actor4j.core.runtime.InternalActorSystem;
import io.actor4j.core.runtime.protocols.ActorProtocolTag;
import io.actor4j.core.utils.ActorGroup;
import io.actor4j.core.utils.ActorGroupSet;
import io.actor4j.core.messages.ActorMessage;
import shared.benchmark.Benchmark;
import shared.benchmark.BenchmarkConfig;

// @see https://salsa.debian.org/benchmarksgame-team/archive-alioth-benchmarksgame/-/blob/master/contributed-source-code/benchmarksgame/threadring/threadring.java-3.java?ref_type=heads
// @see https://github.com/shamsimam/savina/blob/master/src/main/java/edu/rice/habanero/benchmarks/threadring/ThreadRingConfig.java
// @see https://github.com/shamsimam/savina/blob/master/src/main/scala/edu/rice/habanero/benchmarks/threadring/ThreadRingAkkaActorBenchmark.scala
public class BenchmarkActorRingInter extends BenchmarkSampleActor4j {
	public static CountDownLatch latch;
	
	public BenchmarkActorRingInter(BenchmarkConfig config) {
		super(config);

		ActorSystem system = createActorSystem("actor4j::ActorRing-Inter");
		
		System.out.printf("activeThreads: %d%n", config.parallelism());
		System.out.printf("Benchmark started (%s)...%n", system.getConfig().name());
		System.out.printf("#actors: %d%n", config.numberOfActors*config.parallelism());
		System.out.printf("#hops: %s%n", config.param1);
		
		system.start();
		
		Benchmark benchmark = new Benchmark(config);
		
		benchmark.start((timeMeasurement, iteration) -> {
			latch = new CountDownLatch(config.parallelism());
			
			timeMeasurement.start();
			final List<UUID> ringList = new ArrayList<>(config.parallelism());
			for (int j=0; j<config.parallelism(); j++) {
				ActorGroup group = new ActorGroupSet();
				
				UUID next = system.addActor(() -> new Forwarder(group));
				group.add(next);
				for(int i=0; i<config.numberOfActors-2; i++) {
					final UUID next_ = next;
					next = system.addActor(() -> new Forwarder(group, next_));
					group.add(next);
				}
				final UUID next_ = next;
				UUID sender = system.addActor(() -> new Sender(group, next_));
				group.add(sender);
			
				ringList.add(sender);
			}
			for (UUID sender : ringList)
				system.send(ActorMessage.create(null, Integer.valueOf(config.param1), system.SYSTEM_ID(), sender));
			try {
				latch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			timeMeasurement.stop(); // --> stop measurement!
			
			PseudoActor pseudoActor = new PseudoActor(system, true) {
				@Override
				public void preStart() {
					for (UUID sender : ringList)
						watch(sender);
				}
				@Override
				public void receive(ActorMessage<?> message) {
					System.out.println(message);
				}
			};
			
			system.send(ActorMessage.create(null, ActorProtocolTag.INTERNAL_STOP_USER_SPACE, null, system.USER_ID())); // stop all actors within user
			boolean success = true;
			for (int j=0; j<config.parallelism(); j++)
				try {
					boolean temp = pseudoActor.await(
							(msg) -> msg.tag()==Actor.TERMINATED, 
							(msg) -> true, 
							120_000, TimeUnit.MILLISECONDS);
					if (!temp)
						success = false;
				} catch (InterruptedException | TimeoutException e) {
					e.printStackTrace();
				}
			System.out.println("All rings stopped...");
			pseudoActor.stop();
			
			if (!success) {
				for (UUID sender : ringList)
					System.out.println(((InternalActorSystem)system).getCells().get(sender).getChildren().size());
			}
		});
		
		system.shutdown();
	}
	
	public static void main(String[] args) {
		new BenchmarkActorRingInter(new BenchmarkConfig(8, 10, 60, 8, 1, String.valueOf(5_000_000))); // 10 + 60 iterations!
	}
}
