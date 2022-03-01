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
package actor4j.benchmark.samples.ping.pong.bulk;

import static actor4j.benchmark.samples.ping.pong.bulk.ActorMessageTag.RUN;

import java.util.UUID;

import actor4j.benchmark.Benchmark;
import io.actor4j.corex.XActorSystem;
import io.actor4j.corex.config.XActorSystemConfig;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorGroup;
import io.actor4j.core.utils.ActorGroupSet;
import shared.benchmark.BenchmarkConfig;
import shared.benchmark.BenchmarkSample;

public class BenchmarkPingPongBulk extends BenchmarkSample {
	public BenchmarkPingPongBulk(BenchmarkConfig benchmarkConfig) {
		super();
		
		XActorSystemConfig config = XActorSystemConfig.builder()
			.name("actor4j::PingPong-Bulk")
			.sleepMode()
			.build();
		XActorSystem system = new XActorSystem(config);

		ActorGroup group = new ActorGroupSet();
		int size = benchmarkConfig.numberOfActors*benchmarkConfig.parallelism()/2;
		System.out.printf("#actors: %d%n", benchmarkConfig.numberOfActors*benchmarkConfig.parallelism());
		UUID dest = null;
		UUID id = null;
		for(int i=0; i<size; i++) {
			dest = system.addActor(Destination.class);
			id = system.addActor(Client.class, dest);
			group.add(id);
		}
		
		system.broadcast(ActorMessage.create(new Object(), RUN, system.SYSTEM_ID, null), group);
		
		
		Benchmark benchmark = new Benchmark(system, benchmarkConfig);
		benchmark.start();
	}
	
	public static void main(String[] args) {
		new BenchmarkPingPongBulk(new BenchmarkConfig(100));
	}
}
