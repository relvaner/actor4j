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
package jade.benchmark;

import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import actor4j.benchmark.utils.MessageThroughputMeasurement;
import shared.benchmark.BenchmarkConfig;

public class Benchmark {
	protected Jade jade;
	protected Supplier<Long> counter;
	protected BenchmarkConfig config; 
	
	public Benchmark(Jade jade, Supplier<Long> counter, long duration) {
		this(jade, counter, 10, duration);
	}
	
	public Benchmark(Jade jade, Supplier<Long> counter, int warmupIterations, long duration) {
		this(jade, counter, new BenchmarkConfig(warmupIterations, duration));
	}
	
	public Benchmark(Jade jade, Supplier<Long> counter, BenchmarkConfig config) {
		super();
		
		this.jade = jade;
		this.counter = counter;
		this.config = config;
	}
	
	public void start() {
		DescriptiveStatistics statistics = new DescriptiveStatistics();
		AtomicLong warmupCount = new AtomicLong();
		DecimalFormat decimalFormat = new DecimalFormat("###,###,###,###");
		
		System.out.printf("Benchmark started (%s)...%n", jade.getPlatformID());

		MessageThroughputMeasurement messageTM = new MessageThroughputMeasurement(counter, config.warmupIterations, warmupCount, statistics, true);
		messageTM.start();
		
		try {
			Thread.sleep(config.duration+config.warmupIterations*1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		messageTM.stop();
		jade.shutdown();
		
		System.out.printf("statistics::count         : %s%n", decimalFormat.format(counter.get()-warmupCount.get()));
		System.out.printf("statistics::mean::derived : %s msg/s%n", decimalFormat.format((counter.get()-warmupCount.get())/(config.duration/1000)));
		System.out.printf("statistics::mean          : %s msg/s%n", decimalFormat.format(statistics.getMean()));
		System.out.printf("statistics::sd            : %s msg/s%n", decimalFormat.format(statistics.getStandardDeviation()));
		System.out.printf("statistics::median        : %s msg/s%n", decimalFormat.format(statistics.getPercentile(50)));
		System.out.println("Benchmark finished...");	
	}
}