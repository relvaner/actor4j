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

import java.text.DecimalFormat;
import java.util.function.Supplier;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import io.actor4j.benchmark.utils.MessageThroughputMeasurement;

// Framework-neutral throughput benchmark: the framework supplies the message counter and how to stop and shut down.
// Order: measurement (warmup + iterations samples), onStop, statistics, onShutdown.
public class ThroughputBenchmark {
	public static record Result(long count, double perSecond, int samples, DescriptiveStatistics statistics) {
	}

	protected final BenchmarkConfig config;
	protected final String name;
	protected final Supplier<Long> counter;

	protected int activeThreads;
	protected String runtime;
	protected Runnable onStop;
	protected Runnable onShutdown;
	protected boolean console;

	public ThroughputBenchmark(BenchmarkConfig config, String name, Supplier<Long> counter) {
		super();
		this.config = config;
		this.name = name;
		this.counter = counter;

		activeThreads = config.totalParallelism();
		console = true;
	}

	public ThroughputBenchmark activeThreads(int activeThreads) {
		this.activeThreads = activeThreads;
		return this;
	}

	// printed in the header, so that runs of different runtimes are not grouped together
	public ThroughputBenchmark runtime(String runtime) {
		this.runtime = runtime;
		return this;
	}

	// runs after the measurement, before the statistics (e.g. to end busy loops or print counters per thread)
	public ThroughputBenchmark onStop(Runnable onStop) {
		this.onStop = onStop;
		return this;
	}

	// runs at the end, also after a failure
	public ThroughputBenchmark onShutdown(Runnable onShutdown) {
		this.onShutdown = onShutdown;
		return this;
	}

	public ThroughputBenchmark console(boolean console) {
		this.console = console;
		return this;
	}

	public Result start() {
		if (config.iterations()<=0)
			throw new IllegalArgumentException("iterations must be > 0");
		
		final DescriptiveStatistics statistics = new DescriptiveStatistics();
		final DecimalFormat decimalFormat = new DecimalFormat("###,###,###,###");

		print("Logical cores: %d%n", Runtime.getRuntime().availableProcessors());
		print("activeThreads: %d%n", activeThreads);
		if (runtime!=null)
			print("runtime: %s%n", runtime);
		print("Benchmark started (%s)...%n", name);

		MessageThroughputMeasurement measurement = new MessageThroughputMeasurement(counter,
			config.warmupIterations(), config.warmupTime(), config.iterations(), config.measurementTime(), null, statistics, console);
		try {
			measurement.start();
			try {
				measurement.awaitDone();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			measurement.stop();

			if (onStop!=null)
				onStop.run();

			print("statistics::count         : %s%n", decimalFormat.format(measurement.getMeasuredCount()));
			print("statistics::mean::derived : %s msg/s%n", decimalFormat.format(measurement.getMeasuredPerSecond()));
			print("statistics::mean          : %s msg/s%n", decimalFormat.format(statistics.getMean()));
			print("statistics::sd            : %s msg/s%n", decimalFormat.format(statistics.getStandardDeviation()));
			print("statistics::median        : %s msg/s%n", decimalFormat.format(statistics.getPercentile(50)));
			print("statistics::min           : %s msg/s%n", decimalFormat.format(statistics.getMin()));
			print("statistics::max           : %s msg/s%n", decimalFormat.format(statistics.getMax()));
			print("Benchmark finished...%n");

			return new Result(measurement.getMeasuredCount(), measurement.getMeasuredPerSecond(), measurement.getSamples(), statistics);
		}
		finally {
			measurement.stop();
			if (onShutdown!=null)
				onShutdown.run();
		}
	}

	protected void print(String format, Object... args) {
		if (console)
			System.out.printf(format, args);
	}
}
