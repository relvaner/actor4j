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
package io.actor4j.benchmark;

import java.text.DecimalFormat;
import java.util.function.BiConsumer;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import io.actor4j.benchmark.utils.TimeMeasurement;

public class Benchmark {
	protected BenchmarkConfig config;
	protected boolean console;
	
	public Benchmark(int times) {
		this(10, times);
	}
	
	public Benchmark(int warmupIterations, int iterations ) {
		this(new BenchmarkConfig().warmupIterations(warmupIterations).iterations(iterations));
	}
	
	public Benchmark(BenchmarkConfig config) {
		super();
		this.config = config;
		
		console = true;
	}
	
	public Benchmark console(boolean console) {
		this.console = console;
		return this;
	}
	
	// returns the statistics of the measured iterations (in ms)
	public DescriptiveStatistics start(BiConsumer<TimeMeasurement, Integer> runnable) {
		final DescriptiveStatistics statistics = new DescriptiveStatistics();
		
		print("Logical cores: %d%n", Runtime.getRuntime().availableProcessors());
		print("Benchmark started...%n");
		
		final DecimalFormat decimalFormat = new DecimalFormat("###,###,###,##0.000");
		final TimeMeasurement timeMeasurement = new TimeMeasurement();
		for (int i=1; i<=config.warmupIterations()+config.iterations(); i++) {
			if (runnable!=null)
				runnable.accept(timeMeasurement, i);
			if (i>config.warmupIterations()) {
				statistics.addValue(timeMeasurement.getTimeMillisAsDouble());
				print("%-2d : %s ms%n", i-config.warmupIterations(), decimalFormat.format(timeMeasurement.getTimeMillisAsDouble()));
			}
			else
				print("Warmup %-2d : %s ms%n", i, decimalFormat.format(timeMeasurement.getTimeMillisAsDouble()));
			timeMeasurement.reset();
		}
		print("statistics::mean          : %s ms%n", decimalFormat.format(statistics.getMean()));
		print("statistics::sd            : %s ms%n", decimalFormat.format(statistics.getStandardDeviation()));
		print("statistics::median        : %s ms%n", decimalFormat.format(statistics.getPercentile(50)));
		print("statistics::min           : %s ms%n", decimalFormat.format(statistics.getMin()));
		print("statistics::max           : %s ms%n", decimalFormat.format(statistics.getMax()));
		print("Benchmark finished...%n");
		
		return statistics;
	}
	
	protected void print(String format, Object... args) {
		if (console)
			System.out.printf(format, args);
	}
}