/*
 * Copyright (c) 2015-2017, David A. Bauer. All rights reserved.
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
package akka.benchmark;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import actor4j.benchmark.utils.MessageThroughputMeasurement;
import akka.actor.ActorSystem;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import shared.benchmark.BenchmarkConfig;

public class Benchmark {
	protected ActorSystem system;
	protected Supplier<Long> counter;
	protected BenchmarkConfig config;

	public Benchmark(ActorSystem system, Supplier<Long> counter, long duration) {
		this(system, counter, 10, duration);
	}
	
	public Benchmark(ActorSystem system, Supplier<Long> counter, int warmupIterations, long duration) {
		this(system, counter, new BenchmarkConfig(warmupIterations, duration));
	}
	
	public Benchmark(ActorSystem system, Supplier<Long> counter, BenchmarkConfig config) {
		super();
		
		this.system = system;
		this.counter = counter;
		this.config = config;
	}
	
	public void start() {
		DescriptiveStatistics statistics = new DescriptiveStatistics();
		AtomicLong warmupCount = new AtomicLong();
		DecimalFormat decimalFormat = new DecimalFormat("###,###,###,###");
		
		System.out.printf("Logical cores: %d%n", Runtime.getRuntime().availableProcessors());
		System.out.printf("activeThreads: %d%n", config.parallelism());
		System.out.printf("Benchmark started (%s)...%n", system.name());

		MessageThroughputMeasurement messageTM = new MessageThroughputMeasurement(counter, config.warmupIterations, warmupCount, statistics, true);
		messageTM.start();
		
		try {
			Thread.sleep(config.getDuration()+config.warmupIterations*1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		messageTM.stop();
		/*
		system.shutdown();
		system.awaitTermination();
		*/
		try {
			Await.result(system.terminate(), Duration.create(30, TimeUnit.SECONDS));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.printf("statistics::count         : %s%n", decimalFormat.format(counter.get()-warmupCount.get()));
		System.out.printf("statistics::mean::derived : %s msg/s%n", decimalFormat.format((counter.get()-warmupCount.get())/(config.getDuration()/1000)));
		System.out.printf("statistics::mean          : %s msg/s%n", decimalFormat.format(statistics.getMean()));
		System.out.printf("statistics::sd            : %s msg/s%n", decimalFormat.format(statistics.getStandardDeviation()));
		System.out.printf("statistics::median        : %s msg/s%n", decimalFormat.format(statistics.getPercentile(50)));
		System.out.println("Benchmark finished...");	
	}
}