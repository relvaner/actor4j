/*
 * Copyright (c) 2015-2026, David A. Bauer. All rights reserved.
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

import java.util.function.BiConsumer;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import io.actor4j.benchmark.report.BenchmarkOutput;
import io.actor4j.benchmark.report.BenchmarkResult;
import io.actor4j.benchmark.utils.MeasurementEvent;
import io.actor4j.benchmark.utils.TimeMeasurement;
import io.actor4j.benchmark.utils.Timeouts;

public class Benchmark {
	protected BenchmarkConfig config;
	protected boolean console;
	
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
		config.validateIterations();
		final DescriptiveStatistics statistics = new DescriptiveStatistics();
		
		final String name = config.propertyOrDefault("benchmark", config.propertyOrDefault("scenario", "benchmark"));
		print("%s", BenchmarkOutput.header(config, name, "Time per iteration", "ms", config.totalParallelism(), false));

		// -w and -r set the duration of an iteration, here each iteration is one complete run
		if (config.warmupTime()!=BenchmarkConfig.DEFAULT_TIME || config.measurementTime()!=BenchmarkConfig.DEFAULT_TIME)
			System.err.println("Warning: -w and -r have no effect for time scenarios (one run per iteration)");

		final TimeMeasurement timeMeasurement = new TimeMeasurement();
		// JFR event from the first to the end of the last measurement iteration (profilers leave out the warmup)
		final MeasurementEvent event = new MeasurementEvent();
		for (int i=1; i<=config.warmupIterations()+config.iterations(); i++) {
			if (i==config.warmupIterations()+1)
				event.begin();
			if (runnable!=null) {
				final int iteration = i;
				// with a time limit (-to): a hanging scenario ends the run with an error instead of waiting forever
				Timeouts.run(() -> runnable.accept(timeMeasurement, iteration), config.timeout(), String.format("%s: iteration %d", name, i));
				// a forgotten start() or stop() would silently give 0 ms or a wrong time
				if (!timeMeasurement.isMeasured())
					throw new IllegalStateException(String.format("%s: iteration %d without %s of the TimeMeasurement",
						name, i, timeMeasurement.isMeasuring() ? "stop()" : "start() and stop()"));
			}
			boolean warmup = i<=config.warmupIterations();
			if (!warmup)
				statistics.addValue(timeMeasurement.getTimeMillisAsDouble());
			print("%s", BenchmarkOutput.iteration(warmup, warmup ? i : i-config.warmupIterations(), timeMeasurement.getTimeMillisAsDouble(), "ms"));
			timeMeasurement.reset();
		}
		event.commit();
		print("%s", BenchmarkOutput.result(name, statistics, "ms", null));
		
		BenchmarkResult.record(config, config.propertyOrDefault("scenario", "benchmark"), BenchmarkResult.MODE_TIME, "ms", statistics, null);
		
		return statistics;
	}
	
	protected void print(String format, Object... args) {
		if (console)
			System.out.printf(format, args);
	}
}