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
package actor4j.benchmark.samples.fib;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import actor4j.benchmark.BenchmarkSampleActor4j;
import io.actor4j.core.ActorSystem;
import io.actor4j.core.actors.Actor;
import io.actor4j.core.actors.PseudoActor;
import io.actor4j.core.runtime.InternalActorSystem;
import io.actor4j.core.messages.ActorMessage;
import shared.benchmark.Benchmark;
import shared.benchmark.BenchmarkConfig;

public class BenchmarkFib extends BenchmarkSampleActor4j {
	public static CountDownLatch latch;
	
	public BenchmarkFib(BenchmarkConfig config) {
		super(config);

		ActorSystem system = createActorSystem("actor4j::Fibonacci");
		
		System.out.printf("activeThreads: %d%n", config.parallelism());
		System.out.printf("Benchmark started (%s)...%n", system.getConfig().name());
		
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() { 
			@Override
			public void run() {
				System.out.printf("#actors : %s%n", Fibonacci.count);
//				System.out.printf("#cells  : %s%n", ((InternalActorSystem)system).getCells().size());
			}
		}, 1000, 1000);
		
		system.start();
		
		Benchmark benchmark = new Benchmark(config);
		
		benchmark.start((timeMeasurement, iteration) -> {
			latch = new CountDownLatch(1);
			
			timeMeasurement.start();
			UUID fibonacci = system.addActor(() -> new Fibonacci(Long.valueOf(config.param1)));
			system.send(ActorMessage.create(null, Fibonacci.CREATE, system.SYSTEM_ID(), fibonacci));
			try {
				latch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			PseudoActor pseudoActor = new PseudoActor(system, true) {
				@Override
				public void preStart() {
					watch(fibonacci);
				}
				@Override
				public void receive(ActorMessage<?> message) {
					System.out.println(message);
				}
			};
			
			system.send(ActorMessage.create(null, Actor.POISONPILL, null, fibonacci)); // stop all actors from parent
			boolean success = false;
			try {
				success = pseudoActor.await(
						(msg) -> msg.tag()==Actor.TERMINATED, 
						(msg) -> { System.out.println("Fibonacci stopped..."); return true;}, 
						120_000, TimeUnit.MILLISECONDS);
			} catch (InterruptedException | TimeoutException e) {
				e.printStackTrace();
			}
			pseudoActor.stop();
			timeMeasurement.stop();
			
			if (!success) {
				System.out.println(((InternalActorSystem)system).getCells().get(fibonacci).getChildren().size());
			}
			
			System.out.printf("#actors : %s%n", Fibonacci.count);
			Fibonacci.count.getAndSet(0);
		});
		
		timer.cancel();
		system.shutdown();
	}
	
	public static void main(String[] args) {
		new BenchmarkFib(new BenchmarkConfig(10, 60, "30")); // 10 + 60 iterations!
	}
}
