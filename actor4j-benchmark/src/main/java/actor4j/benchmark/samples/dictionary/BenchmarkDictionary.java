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
package actor4j.benchmark.samples.dictionary;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

import io.actor4j.core.ActorSystem;
import io.actor4j.core.XActorSystemImpl;
import io.actor4j.core.actors.Actor;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorFactory;
import io.actor4j.core.data.access.utils.VolatileActorCacheManager;
import shared.benchmark.Benchmark;
import shared.benchmark.BenchmarkConfig;
import shared.benchmark.BenchmarkSample;

// @see https://hackernoon.com/what-to-do-with-5-000-000-akka-actors-381a915a0f78
public class BenchmarkDictionary extends BenchmarkSample {
	public BenchmarkDictionary(BenchmarkConfig config) {
		super();
		
		final int attempts = 2_500_000;//2_500_000;
		final int keySize  = 5_000_000;//5_000_000;
		
		ActorSystem system = new ActorSystem("actor4j::Dictionary", XActorSystemImpl.class);
		if (config.parallelismMin>0)
			system.setParallelismMin(config.parallelismMin);
		if (config.parallelismFactor>0)
			system.setParallelismFactor(config.parallelismFactor);
		system.underlyingImpl().setQueueSize(5_000_000);
		system.underlyingImpl().setBufferQueueSize(1_000_000);
		final int COUNT = config.parallelism();

		CountDownLatch testDone = new CountDownLatch(1);

		ActorFactory factory = () -> new Actor() {
			protected VolatileActorCacheManager<Integer, Integer> manager;
			protected int i = 0;
			protected int j = 0;

			@Override
			public void preStart() {
				manager = new VolatileActorCacheManager<Integer, Integer>(this, "vcache1");
				system.addActor(VolatileActorCacheManager.create(COUNT - 1, attempts, "vcache1"));
				Random random = new Random();
				for (int i=0; i<attempts; i++)
					manager.set(random.nextInt(keySize), random.nextInt());
				for (int i=0; i<keySize; i++)
					manager.get(random.nextInt(keySize));
			}

			@Override
			public void receive(ActorMessage<?> message) {
				if (manager.get(message)!=null)
					j++;
				i++;
				if (i>=keySize) {
					System.out.printf("found: %d%n", j);
					testDone.countDown();
				}
			}
		};
		
		Benchmark benchmark = new Benchmark(config);

		benchmark.start((timeMeasurement, iteration) -> {
			timeMeasurement.start();
				system.addActor(factory);
				system.start();
				try {
					testDone.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			timeMeasurement.stop();
			
				system.shutdownWithActors(true);
		});
		
	}

	public static void main(String[] args) {
		new BenchmarkDictionary(new BenchmarkConfig(10, 60));
	}
}
