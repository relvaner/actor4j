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
package actor4j.benchmark.samples.ring.nfold;

import java.util.UUID;

import actor4j.benchmark.Benchmark;
import io.actor4j.corex.XActorSystem;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorGroup;
import io.actor4j.core.utils.ActorGroupSet;
import shared.benchmark.BenchmarkConfig;
import shared.benchmark.BenchmarkSample;

public class BenchmarkRingNfold extends BenchmarkSample {
	public BenchmarkRingNfold(BenchmarkConfig config) {
		super();
		
		XActorSystem system = new XActorSystem("actor4j::NFoldRing");
		system.sleepMode();
		
		System.out.printf("#actors: %d%n", config.numberOfActors*config.parallelism());
		for (int j=0; j<config.parallelism(); j++) {
			ActorGroup group = new ActorGroupSet();
			
			UUID next = system.addActor(Forwarder.class, group);
			group.add(next);
			for(int i=0; i<config.numberOfActors-2; i++) {
				next = system.addActor(Forwarder.class, group, next);
				group.add(next);
			}
			UUID sender = system.addActor(Sender.class, group, next);
			group.add(sender);
		
			system.send(new ActorMessage<>(new String("DUMMY"), 0, sender, sender));
		}
		
		Benchmark benchmark = new Benchmark(system, config);
		benchmark.start();
	}
	
	public static void main(String[] args) {
		new BenchmarkRingNfold(new BenchmarkConfig(100));
	}
}
