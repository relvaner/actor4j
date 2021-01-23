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
package jade.benchmark.samples.ping.pong.bulk;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import jade.benchmark.Benchmark;
import jade.benchmark.BenchmarkAgent;
import jade.benchmark.Jade;
import jade.core.AID;
import shared.benchmark.BenchmarkConfig;
import shared.benchmark.BenchmarkSample;

public class BenchmarkPingPongBulk extends BenchmarkSample {
	public BenchmarkPingPongBulk(BenchmarkConfig config) {
		super();
		
		Jade jade = new Jade("jade-benchmark-ping-pong-bulk");
		jade.start();
		
		int size = config.numberOfActors/2;
		System.out.printf("#agents: %d%n", config.numberOfActors);
		
		List<AID> ports = new ArrayList<>();
		for(int i=0; i<size; i++) {
			jade.addAgent("ping"+i, Ping.class.getName(), new Object[] {"pong"+i});
			jade.addAgent("pong"+i, Pong.class.getName(), null);
			ports.add(new AID("ping"+i, AID.ISLOCALNAME));
		}
		
		jade.addAgent("hub", Hub.class.getName(), new Object[] {ports});
	
		Benchmark benchmark = new Benchmark(jade, new Supplier<Long>() {
			@Override
			public Long get() {
				return BenchmarkAgent.counter.get();
			}
		}, config);
		benchmark.start();
	}
	
	public static void main(String[] args) {
		new BenchmarkPingPongBulk(new BenchmarkConfig(100));
	}
}
