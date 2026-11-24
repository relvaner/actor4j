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

import java.util.Map;
import java.util.function.Supplier;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import io.actor4j.benchmark.report.BenchmarkOutput;
import io.actor4j.benchmark.report.BenchmarkResult;
import io.actor4j.benchmark.utils.MessageThroughputMeasurement;
import io.actor4j.benchmark.utils.Timeouts;

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

	// runs after the measurement, before the statistics (e.g. to end busy loops or print counters per thread),
	// also after a failure of the counter, with the time limit -to
	public ThroughputBenchmark onStop(Runnable onStop) {
		this.onStop = onStop;
		return this;
	}

	// runs at the end, also after a failure, with the time limit -to
	public ThroughputBenchmark onShutdown(Runnable onShutdown) {
		this.onShutdown = onShutdown;
		return this;
	}

	public ThroughputBenchmark console(boolean console) {
		this.console = console;
		return this;
	}

	public Result start() {
		config.validateIterations();
		
		final DescriptiveStatistics statistics = new DescriptiveStatistics();
		if (runtime!=null && config.property("runtime")==null)
			config.property("runtime", runtime);
		final String benchmark = config.propertyOrDefault("benchmark", name);
		print("%s", BenchmarkOutput.header(config, benchmark, "Throughput", "msg/s", activeThreads, true));

		MessageThroughputMeasurement measurement = new MessageThroughputMeasurement(counter,
			config.warmupIterations(), config.warmupTime(), config.iterations(), config.measurementTime(), statistics, console);
		// the measurement takes a fixed time (-wi, -w, -i, -r), plus the timeout (-to) as buffer
		final long limit = config.warmupIterations()*config.warmupTime()+config.iterations()*config.measurementTime()+config.timeout();
		Throwable failure = null;
		try {
			measurement.start();
			boolean done = true;
			try {
				done = measurement.awaitDone(limit);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			measurement.stop();
			if (!done)
				throw new IllegalStateException(String.format("Measurement did not end within %s (-to)", Timeouts.format(limit)));
			// a failed counter ends the measurement early: fail the run instead of reporting partial values,
			// onStop still runs (it may end parts of the scenario), its own failure is added as suppressed
			if (measurement.getFailure()!=null) {
				IllegalStateException e = new IllegalStateException("Message counter failed: "+measurement.getFailure(), measurement.getFailure());
				try {
					runOnStop();
				} catch (RuntimeException | Error stopFailure) {
					e.addSuppressed(stopFailure);
				}
				throw e;
			}

			runOnStop();

			print("%s", BenchmarkOutput.result(benchmark, statistics, "msg/s", String.format("derived: %s msg/s, count: %s msgs",
				BenchmarkOutput.format(measurement.getMeasuredPerSecond()), BenchmarkOutput.format(measurement.getMeasuredCount()))));

			BenchmarkResult.record(config, name, BenchmarkResult.MODE_THROUGHPUT, "msg/s", statistics, Map.of(
				"derived", new Object[] { measurement.getMeasuredPerSecond(), "msg/s" },
				"count", new Object[] { measurement.getMeasuredCount(), "msgs" }));

			return new Result(measurement.getMeasuredCount(), measurement.getMeasuredPerSecond(), measurement.getSamples(), statistics);
		}
		catch (RuntimeException | Error e) {
			failure = e;
			throw e;
		}
		finally {
			measurement.stop();
			if (onShutdown!=null)
				try {
					Timeouts.run(onShutdown, config.timeout(), "onShutdown");
				} catch (RuntimeException | Error e) {
					// a failure of the run stays the primary exception
					if (failure!=null)
						failure.addSuppressed(e);
					else
						throw e;
				}
		}
	}

	protected void runOnStop() {
		if (onStop!=null)
			Timeouts.run(onStop, config.timeout(), "onStop");
	}

	protected void print(String format, Object... args) {
		if (console)
			System.out.printf(format, args);
	}
}
