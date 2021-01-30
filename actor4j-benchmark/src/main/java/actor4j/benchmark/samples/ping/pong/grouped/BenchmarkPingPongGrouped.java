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
package actor4j.benchmark.samples.ping.pong.grouped;

import static actor4j.benchmark.samples.ping.pong.grouped.ActorMessageTag.MSG;

import java.util.UUID;

import actor4j.benchmark.Benchmark;
import io.actor4j.corex.XActorSystem;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorGroup;
import io.actor4j.core.utils.ActorGroupSet;
import shared.benchmark.BenchmarkConfig;
import shared.benchmark.BenchmarkSample;

public class BenchmarkPingPongGrouped extends BenchmarkSample {
	public BenchmarkPingPongGrouped(BenchmarkConfig config) {
		super();
		
		XActorSystem system = new XActorSystem("actor4j::PingPong-Grouped");
		system.sleepMode();
		
		ActorGroup group = new ActorGroupSet();
		ActorGroup[] groups = new ActorGroup[config.parallelism()];
		for (int i=0; i<groups.length; i++)
			groups[i] = new ActorGroupSet();
		int size = config.numberOfActors*config.parallelism()/2;
		System.out.printf("#actors: %d%n", config.numberOfActors*config.parallelism());
		UUID dest = null;
		UUID id = null;
		for(int i=0; i<size; i++) {
			dest = system.addActor(Destination.class, groups[i%config.parallelism()]);
			id = system.addActor(Client.class, groups[(i+1)%config.parallelism()], dest);
			group.add(id);
		}
		
		system.broadcast(new ActorMessage<Object>(new Object(), MSG, dest, null), group);
		
		Benchmark benchmark = new Benchmark(system, config);
		benchmark.start();
	}
	
	public static void main(String[] args) {
		new BenchmarkPingPongGrouped(new BenchmarkConfig(100));
	}
}
