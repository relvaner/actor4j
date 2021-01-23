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
package akka.benchmark.samples.bcast;

import java.util.function.Supplier;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.benchmark.ActorMessage;
import akka.benchmark.Benchmark;
import akka.benchmark.BenchmarkSampleAkka;
import shared.benchmark.BenchmarkConfig;

public class BenchmarkBcast extends BenchmarkSampleAkka {
	public BenchmarkBcast(BenchmarkConfig config) {
		super(config);
		
		ActorSystem system = ActorSystem.create("akka-benchmark-bcast", akkaConfig);
		
		final HubPattern hub = new HubPattern();
		int size = config.numberOfActors*config.parallelism();
		ActorRef ref = null;
		for(int i=0; i<size; i++) {
			ref = system.actorOf(Props.create(TestActor.class, hub).withDispatcher("my-dispatcher"));
			hub.add(ref);
		}
		hub.broadcast(new ActorMessage(0), ref);
		
		Benchmark benchmark = new Benchmark(system, new Supplier<Long>() {
			@Override
			public Long get() {
				return hub.getCount();
			}
		}, config);
		benchmark.start();
	}
	
	public static void main(String[] args) {
		new BenchmarkBcast(new BenchmarkConfig(100));
	}
}
