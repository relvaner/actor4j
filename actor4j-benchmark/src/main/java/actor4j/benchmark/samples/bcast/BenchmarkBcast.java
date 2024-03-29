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
package actor4j.benchmark.samples.bcast;

import java.util.UUID;

import actor4j.benchmark.Benchmark;
import actor4j.benchmark.BenchmarkSampleActor4j;
import io.actor4j.core.ActorSystem;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorGroup;
import io.actor4j.core.utils.ActorGroupSet;
import shared.benchmark.BenchmarkConfig;

public class BenchmarkBcast extends BenchmarkSampleActor4j {
	public BenchmarkBcast(BenchmarkConfig config) {
		super(config);
		
		ActorSystem system = createActorSystem("actor4j::Bcast");
		
		final ActorGroup group = new ActorGroupSet();
		int size = config.numberOfActors*config.parallelism();
		System.out.printf("#actors: %d%n", size);
		UUID id = null;
		for(int i=0; i<size; i++) {
			id = system.addActor(() -> new TestActor(group));
			group.add(id);
		}
		
		system.broadcast((ActorMessage.create(new Object(), 0, id, null)), group);
		
		
		Benchmark benchmark = new Benchmark(system, config);
		benchmark.start();
	}
	
	public static void main(String[] args) {
		new BenchmarkBcast(new BenchmarkConfig(100));
	}
}
