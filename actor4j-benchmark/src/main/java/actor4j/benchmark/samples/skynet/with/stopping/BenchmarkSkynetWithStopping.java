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
package actor4j.benchmark.samples.skynet.with.stopping;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import io.actor4j.corex.XActorSystem;
import io.actor4j.corex.config.XActorSystemConfig;
import io.actor4j.core.messages.ActorMessage;
import shared.benchmark.Benchmark;
import shared.benchmark.BenchmarkConfig;
import shared.benchmark.BenchmarkSample;

// @see https://github.com/atemerev/skynet
// @see https://dzone.com/articles/go-and-quasar-a-comparison-of-style-and-performanc
public class BenchmarkSkynetWithStopping extends BenchmarkSample {
	public static CountDownLatch latch;
	
	public BenchmarkSkynetWithStopping(BenchmarkConfig benchmarkConfig) {
		super();
		
		//ActorSystem system = new ActorSystem("actor4j::SkynetWithStopping", (name, wrapper) -> new AntiFloodingActorSystemImpl(name, wrapper));
		//((AntiFloodingActorSystemImpl)system.underlyingImpl()).setFactoryAntiFloodingTimer(() -> new AntiFloodingTimer(-1, 30_000));
		XActorSystemConfig config = XActorSystemConfig.builder()
			.name("actor4j::SkynetWithStopping")
			.parkMode()
			.build();
		XActorSystem system = new XActorSystem(config);
		
		/*
		system.underlyingImpl().setBufferQueueSize(1_000_000);
		system.underlyingImpl().setQueueSize(5_000_000);
		*/
		
		System.out.printf("activeThreads: %d%n", benchmarkConfig.parallelism());
		System.out.printf("Benchmark started (%s)...%n", system.getConfig().name);
		
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() { 
			@Override
			public void run() {
				System.out.printf("#actors : %s%n", Skynet.count);
			}
		}, 0, 1000);
		
		system.start();
		
		Benchmark benchmark = new Benchmark(benchmarkConfig);
		
		benchmark.start((timeMeasurement, iteration) -> {
			latch = new CountDownLatch(1);
			
			timeMeasurement.start();
			UUID skynet = system.addActor(() -> new Skynet(0, 1_000_000, 10));
			system.send(ActorMessage.create(null, Skynet.CREATE, system.SYSTEM_ID, skynet));
			try {
				latch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			timeMeasurement.stop();
			
			// for without stopping the actors (time not measured of stopping actors)
			/*
			PseudoActor pseudoActor = new PseudoActor(system, true) {
				@Override
				public void preStart() {
					watch(skynet);
				}
				@Override
				public void receive(ActorMessage<?> message) {
				}
			};
			system.send(new ActorMessage<>(null, Actor.POISONPILL, null, skynet));
			try {
				pseudoActor.await(
						(msg) -> msg.tag==Actor.TERMINATED, 
						(msg) -> { System.out.println("Skynet stopped..."); return null;}, 
						10_000, TimeUnit.MILLISECONDS);
			} catch (InterruptedException | TimeoutException e) {
				e.printStackTrace();
			}
			*/
			
			System.out.printf("#actors : %s%n", Skynet.count);
			Skynet.count.getAndSet(0);
		});
		
		timer.cancel();
		system.shutdown();
	}
	
	public static void main(String[] args) {
		new BenchmarkSkynetWithStopping(new BenchmarkConfig(10, 60));
	}
}
