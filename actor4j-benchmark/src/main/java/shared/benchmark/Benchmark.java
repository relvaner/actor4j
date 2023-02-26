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
package shared.benchmark;

import java.text.DecimalFormat;
import java.util.function.BiConsumer;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import tools4j.utils.TimeMeasurement;

public class Benchmark {
	protected BenchmarkConfig config;
	
	public Benchmark(int times) {
		this(10, times);
	}
	
	public Benchmark(int warmupIterations, int times) {
		this(new BenchmarkConfig(warmupIterations, times));
	}
	
	public Benchmark(BenchmarkConfig config) {
		super();
		this.config = config;
	}
	
	public void start(BiConsumer<TimeMeasurement, Integer> runnable) {
		final DescriptiveStatistics statistics = new DescriptiveStatistics();
		
		System.out.printf("Logical cores: %d%n", Runtime.getRuntime().availableProcessors());
		System.out.printf("Benchmark started...%n");
		
		final DecimalFormat decimalFormat = new DecimalFormat("###,###,###,###");
		final TimeMeasurement timeMeasurement = new TimeMeasurement();
		for (int i=1; i<=config.warmupIterations+config.duration/1000; i++) {
			if (runnable!=null)
				runnable.accept(timeMeasurement, i);
			if (statistics!=null && i>config.warmupIterations)
				statistics.addValue(timeMeasurement.getTime());
			if (i>config.warmupIterations)
				System.out.printf("%-2d : %s ms%n", i-config.warmupIterations, decimalFormat.format(timeMeasurement.getTime()));
			else
				System.out.printf("Warmup %-2d : %s ms%n", i, decimalFormat.format(timeMeasurement.getTime()));
			timeMeasurement.reset();
		}
		System.out.printf("statistics::mean          : %s ms%n", decimalFormat.format(statistics.getMean()));
		System.out.printf("statistics::sd            : %s ms%n", decimalFormat.format(statistics.getStandardDeviation()));
		System.out.printf("statistics::median        : %s ms%n", decimalFormat.format(statistics.getPercentile(50)));
		System.out.println("Benchmark finished...");
	}
}