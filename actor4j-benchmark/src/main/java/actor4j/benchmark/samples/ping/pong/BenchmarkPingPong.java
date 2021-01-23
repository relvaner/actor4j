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
package actor4j.benchmark.samples.ping.pong;

import static actor4j.benchmark.samples.ping.pong.ActorMessageTag.MSG;

import java.util.UUID;

import actor4j.benchmark.Benchmark;
import io.actor4j.core.ActorSystem;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorGroup;
import io.actor4j.core.utils.ActorGroupSet;
import shared.benchmark.BenchmarkConfig;
import shared.benchmark.BenchmarkSample;

public class BenchmarkPingPong extends BenchmarkSample {
	public BenchmarkPingPong(BenchmarkConfig config) {
		super();
		
		ActorSystem system = new ActorSystem("actor4j::PingPong");
		system.sleepMode();
		
		ActorGroup group = new ActorGroupSet();
		int size = config.numberOfActors*config.parallelism()/2;
		System.out.printf("#actors: %d%n", config.numberOfActors*config.parallelism());
		UUID dest = null;
		UUID id = null;
		for(int i=0; i<size; i++) {
			dest = system.addActor(Destination.class);
			id = system.addActor(Client.class, dest);
			group.add(id);
		}
		
		system.broadcast(new ActorMessage<Object>(new Object(), MSG, dest, null), group);
		
		
		Benchmark benchmark = new Benchmark(system, config);
		benchmark.start();
	}
	
	public static void main(String[] args) {
		new BenchmarkPingPong(new BenchmarkConfig(100));
	}
}
