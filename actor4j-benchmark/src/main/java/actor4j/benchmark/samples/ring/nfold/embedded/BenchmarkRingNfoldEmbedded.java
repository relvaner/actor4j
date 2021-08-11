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
package actor4j.benchmark.samples.ring.nfold.embedded;

import java.util.UUID;

import actor4j.benchmark.Benchmark;
import io.actor4j.corex.XActorSystem;
import io.actor4j.corex.config.XActorSystemConfig;
import io.actor4j.core.messages.ActorMessage;
import shared.benchmark.BenchmarkConfig;
import shared.benchmark.BenchmarkSample;

public class BenchmarkRingNfoldEmbedded extends BenchmarkSample {
	public BenchmarkRingNfoldEmbedded(BenchmarkConfig benchmarkConfig) {
		super();
		
		XActorSystemConfig config = XActorSystemConfig.builder()
			.name("actor4j::NFoldRingEmbedded")
			.sleepMode()
			.build();
		XActorSystem system = new XActorSystem(config); // use -Xss1g
		
		System.out.printf("#actors: %d%n", benchmarkConfig.numberOfActors*benchmarkConfig.parallelism());
		for (int j=0; j<benchmarkConfig.parallelism(); j++) {
			UUID host = system.addActor(Host.class, benchmarkConfig.numberOfActors);
			system.send(new ActorMessage<>(new String("DUMMY"), 0, host, host));
		}
		
		Benchmark benchmark = new Benchmark(system, benchmarkConfig);
		benchmark.start();
		Host.stop.set(true);
	}
	
	public static void main(String[] args) {
		new BenchmarkRingNfoldEmbedded(new BenchmarkConfig(100));
	}
}
