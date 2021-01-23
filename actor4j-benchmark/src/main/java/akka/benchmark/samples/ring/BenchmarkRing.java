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
package akka.benchmark.samples.ring;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.benchmark.Benchmark;
import akka.benchmark.BenchmarkSampleAkka;
import shared.benchmark.BenchmarkConfig;

public class BenchmarkRing extends BenchmarkSampleAkka {
	public BenchmarkRing(BenchmarkConfig config) {
		super(config);
		
		ActorSystem system = ActorSystem.create("akka-benchmark-ring", akkaConfig);
		
		final AtomicLong counter = new AtomicLong();
		System.out.printf("#actors: %d%n", config.numberOfActors*config.parallelism());
		for (int j=0; j<config.parallelism(); j++) {
			ActorRef next = system.actorOf(Props.create(Forwarder.class).withDispatcher("my-dispatcher"));
			for(int i=0; i<config.numberOfActors-2; i++) {
				next = system.actorOf(Props.create(Forwarder.class, next).withDispatcher("my-dispatcher"));
			}
			ActorRef sender = system.actorOf(Props.create(Sender.class, counter, next, config.numberOfActors).withDispatcher("my-dispatcher"));
			sender.tell(new Object(), sender);
		}
		
		Benchmark benchmark = new Benchmark(system, new Supplier<Long>() {
			@Override
			public Long get() {
				return counter.get();
			}
		}, config);
		benchmark.start();
	}
	
	public static void main(String[] args) {
		new BenchmarkRing(new BenchmarkConfig(100));
	}
}
