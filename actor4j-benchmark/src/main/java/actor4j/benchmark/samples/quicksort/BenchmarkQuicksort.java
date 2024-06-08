/*

 * Copyright (c) 2015-2024, David A. Bauer. All rights reserved.
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
package actor4j.benchmark.samples.quicksort;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.io.FileUtils;

import actor4j.benchmark.BenchmarkSampleActor4j;
import io.actor4j.core.ActorSystem;
import io.actor4j.streams.core.ActorStream;
import io.actor4j.streams.core.ActorStreamManager;
import shared.benchmark.Benchmark;
import shared.benchmark.BenchmarkConfig;

public class BenchmarkQuicksort extends BenchmarkSampleActor4j {
	public static CountDownLatch latch;
	
	public BenchmarkQuicksort(BenchmarkConfig config) {
		super(config);

		ActorSystem system = createActorSystem("actor4j::Quicksort");
		
		System.out.printf("activeThreads: %d%n", config.parallelism());
		System.out.printf("Benchmark started (%s)...%n", system.getConfig().name());
		
		system.start();
		
		Benchmark benchmark = new Benchmark(config);
		
		benchmark.start((timeMeasurement, iteration) -> {
			int data_size = Integer.valueOf(config.param1);
			List<Integer> data = new ArrayList<>(data_size);
			for (int i=0; i<data_size; i++)
				data.add(ThreadLocalRandom.current().nextInt());
			
			timeMeasurement.start();
			ActorStream<Integer, Double> process = new ActorStream<>();
			process
				.data(data)
//				.filter(v -> v>0)
//				.map(v -> v+100d)
//				.forEach(System.out::println)
				.sortedASC();
			
			ActorStreamManager manager = new ActorStreamManager(system);
			manager
				.onStartup(() -> system.start())
				.start(process);
			timeMeasurement.stop();
			try {
				FileUtils.writeStringToFile(new File("actor4j_quicksort.txt"), manager.getFirstResult().toString(), Charset.forName("UTF-8"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		
		system.shutdown();
	}
	
	public static void main(String[] args) {
		new BenchmarkQuicksort(new BenchmarkConfig(0, 10, 60, 8, 1, String.valueOf(20_000_000))); // 10 + 60 iterations!
	}
}
