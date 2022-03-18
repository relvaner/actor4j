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
package jade.benchmark.samples.ring.nfold;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import jade.benchmark.Benchmark;
import jade.benchmark.BenchmarkAgent;
import jade.benchmark.Jade;
import jade.benchmark.samples.ping.pong.Hub;
import jade.core.AID;
import shared.benchmark.BenchmarkConfig;
import shared.benchmark.BenchmarkSample;

public class BenchmarkRingNfold extends BenchmarkSample {
	public BenchmarkRingNfold(BenchmarkConfig config) {
		super(config);
		
		Jade jade = new Jade("jade-benchmark-nfold-ring");
		jade.start();
		
		int size = config.numberOfActors;
		int count = config.parallelism();
		System.out.printf("#agents: %d%n", size*count);
		List<AID> ports = new ArrayList<>();
		for(int j=0; j<count; j++) {
			String last = "forwarder"+(j*size);
			jade.addAgent("forwarder"+(j*size), Forwarder.class.getName(), null);
			int i=0;
			for(; i<size-2; i++) {
				jade.addAgent("forwarder"+(j*size+i+1), Forwarder.class.getName(), new Object[] {"forwarder"+(j*size+i)});
				last = "forwarder"+(j*size+i+1);
			}
			jade.addAgent("sender"+j, Sender.class.getName(), new Object[] {last});
			ports.add(new AID("sender"+j, AID.ISLOCALNAME));
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
		new BenchmarkRingNfold(new BenchmarkConfig(100));
	}
}
