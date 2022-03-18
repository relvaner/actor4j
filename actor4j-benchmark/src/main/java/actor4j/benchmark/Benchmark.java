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
package actor4j.benchmark;

import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import actor4j.benchmark.utils.MessageThroughputMeasurement;
import io.actor4j.core.ActorSystem;
import io.actor4j.core.internal.DefaultActorSystemImpl;
import shared.benchmark.BenchmarkConfig;

public class Benchmark {
	protected ActorSystem system;
	protected BenchmarkConfig benchmarkConfig;
	
	protected MessageThroughputMeasurement messageTM;
	
	public Benchmark(ActorSystem system, long duration) {
		this(system, 10, duration);
	}
	
	public Benchmark(ActorSystem system, int warmupIterations, long duration) {
		this(system,  new BenchmarkConfig(warmupIterations, duration));
	}
	
	public Benchmark(ActorSystem system, BenchmarkConfig benchmarkConfig) {
		super();
		
		this.system = system;
		this.benchmarkConfig = benchmarkConfig;
	}
	
	public void start() {
		final DescriptiveStatistics statistics = new DescriptiveStatistics();
		final AtomicLong warmupCount = new AtomicLong();
		
		System.out.printf("Logical cores: %d%n", Runtime.getRuntime().availableProcessors());
		System.out.printf("activeThreads: %d%n", system.getConfig().parallelism*system.getConfig().parallelismFactor);
		System.out.printf("Benchmark started (%s)...%n", system.getConfig().name);
		system.start(null, new Runnable() {
			@Override
			public void run() {
				DecimalFormat decimalFormat = new DecimalFormat("###,###,###,###");
				
				int i=0;
				for (long value : system.underlyingImpl().getExecuterService().getCounts()) {
					System.out.printf("actor4j-worker-thread-%d::count = %s%n", i, decimalFormat.format(value));
					i++;
				}
				if (system.underlyingImpl() instanceof DefaultActorSystemImpl) {
					i=0;
					for (int value : ((DefaultActorSystemImpl)system.underlyingImpl()).getWorkerInnerQueueSizes()) {
						System.out.printf("actor4j-worker-thread-%d::inner::queue::size = %s%n", i, decimalFormat.format(value));
						i++;
					}
					i=0;
					for (int value : ((DefaultActorSystemImpl)system.underlyingImpl()).getWorkerOuterQueueSizes()) {
						System.out.printf("actor4j-worker-thread-%d::outer::queue::size = %s%n", i, decimalFormat.format(value));
						i++;
					}
				}
				System.out.printf("statistics::count         : %s%n", decimalFormat.format(system.underlyingImpl().getExecuterService().getCount()-warmupCount.get()));
				System.out.printf("statistics::mean::derived : %s msg/s%n", decimalFormat.format((system.underlyingImpl().getExecuterService().getCount()-warmupCount.get())/(benchmarkConfig.getDuration()/1000)));
				System.out.printf("statistics::mean          : %s msg/s%n", decimalFormat.format(statistics.getMean()));
				System.out.printf("statistics::sd            : %s msg/s%n", decimalFormat.format(statistics.getStandardDeviation()));
				System.out.printf("statistics::median        : %s msg/s%n", decimalFormat.format(statistics.getPercentile(50)));
				System.out.println("Benchmark finished...");
			}
		});
		
		messageTM = new MessageThroughputMeasurement(new Supplier<Long>() {
			@Override
			public Long get() {
				return system.underlyingImpl().getExecuterService().getCount();
			}
		}, benchmarkConfig.warmupIterations, warmupCount, statistics, true);
		messageTM.start();
		
		try {
			Thread.sleep(benchmarkConfig.getDuration()+benchmarkConfig.warmupIterations*1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		messageTM.stop();
		system.shutdown(true);
	}
	
	public void stop() {
		messageTM.stop();
	}
}