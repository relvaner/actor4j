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
package akka.benchmark.samples.ping.pong;

import static akka.benchmark.samples.ping.pong.ActorMessageTag.*;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.benchmark.ActorMessage;
import akka.benchmark.Benchmark;
import akka.benchmark.BenchmarkSampleAkka;
import shared.benchmark.BenchmarkConfig;

public class BenchmarkPingPong extends BenchmarkSampleAkka {
	public BenchmarkPingPong(BenchmarkConfig config) {
		super(config);
		
		ActorSystem system = ActorSystem.create("akka-benchmark-ping-pong", akkaConfig);
		
		final AtomicLong counter = new AtomicLong();
		
		HubPattern hub = new HubPattern();
		int size = config.numberOfActors*config.parallelism()/2;
		System.out.printf("#actors: %d%n", config.numberOfActors*config.parallelism());
		ActorRef dest = null;
		ActorRef ref = null;
		for(int i=0; i<size; i++) {
			dest = system.actorOf(Props.create(Destination.class, counter).withDispatcher("my-dispatcher"));
			ref = system.actorOf(Props.create(Client.class, dest).withDispatcher("my-dispatcher"));
			hub.add(ref);
		}
		hub.broadcast(new ActorMessage(MSG), dest);
		
		Benchmark benchmark = new Benchmark(system, new Supplier<Long>() {
			@Override
			public Long get() {
				return counter.get();
			}
		}, config);
		benchmark.start();
	}
	
	public static void main(String[] args) {
		new BenchmarkPingPong(new BenchmarkConfig(100));
	}
}
