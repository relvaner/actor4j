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
package io.actor4j.benchmark.report;

import java.lang.management.ManagementFactory;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import io.actor4j.benchmark.BenchmarkConfig;
import io.actor4j.benchmark.utils.Timeouts;

// Console output like JMH: header with # lines, one line per iteration, result block with ±(99.9%).
// Always in English (numbers 1,234.5), independent of the locale of the JVM.
public final class BenchmarkOutput {
	private BenchmarkOutput() {
	}

	// fixed English number format (1,234.5), independent of the locale of the JVM, so that logs are comparable
	private static final DecimalFormatSymbols SYMBOLS = DecimalFormatSymbols.getInstance(Locale.ENGLISH);

	public static String format(double value) {
		return new DecimalFormat("#,##0.###", SYMBOLS).format(value);
	}

	// mode: "Throughput" or "Time per iteration", timed: iterations with a duration (throughput)
	public static String header(BenchmarkConfig config, String name, String mode, String unit, int threads, boolean timed) {
		StringBuilder b = new StringBuilder();
		b.append(String.format(Locale.ENGLISH, "# VM version: JDK %s, %s, %s%n", System.getProperty("java.version"), System.getProperty("java.vm.name"), System.getProperty("java.vm.version")));
		b.append(String.format(Locale.ENGLISH, "# VM options: %s%n", ManagementFactory.getRuntimeMXBean().getInputArguments().isEmpty() ? "<none>" : String.join(" ", ManagementFactory.getRuntimeMXBean().getInputArguments())));
		b.append(String.format(Locale.ENGLISH, "# Warmup: %d iterations%s%n", config.warmupIterations(), timed ? ", "+time(config.warmupTime())+" each" : ""));
		b.append(String.format(Locale.ENGLISH, "# Measurement: %d iterations%s%n", config.iterations(), timed ? ", "+time(config.measurementTime())+" each" : ""));
		b.append(String.format(Locale.ENGLISH, "# Timeout: %s per iteration%n", time(config.timeout())));
		b.append(String.format(Locale.ENGLISH, "# Worker threads: %d (logical cores: %d)%n", threads, Runtime.getRuntime().availableProcessors()));
		b.append(String.format(Locale.ENGLISH, "# Benchmark mode: %s, %s%n", mode, unit));
		b.append(String.format(Locale.ENGLISH, "# Benchmark: %s%n", config.propertyOrDefault("benchmark", name)));
		b.append(String.format(Locale.ENGLISH, "# Parameters: %s%n", parameters(config)));
		for (String line : config.header())
			b.append(String.format(Locale.ENGLISH, "# %s%n", line));
		b.append(String.format(Locale.ENGLISH, "%n"));
		return b.toString();
	}

	protected static String time(long millis) {
		return Timeouts.format(millis);
	}

	protected static String parameters(BenchmarkConfig config) {
		Map<String, String> params = new LinkedHashMap<>();
		if (config.property("runtime")!=null)
			params.put("runtime", config.property("runtime"));
		params.put("actors", String.valueOf(config.numberOfActors()));
		params.put("mode", config.threadMode());
		params.put("throughput", String.valueOf(config.throughput()));
		params.putAll(config.params());
		StringJoiner joiner = new StringJoiner(", ", "(", ")");
		params.forEach((k, v) -> joiner.add(k+" = "+v));
		return joiner.toString();
	}

	public static String iteration(boolean warmup, int iteration, double value, String unit) {
		return String.format(Locale.ENGLISH, warmup ? "# Warmup Iteration %3d: %s %s%n" : "Iteration %3d: %s %s%n", iteration, format(value), unit);
	}

	// extra: additional lines below the result (e.g. derived throughput), may be null
	public static String result(String benchmark, DescriptiveStatistics statistics, String unit, String extra) {
		double mean = statistics.getMean();
		double error = BenchmarkResult.scoreError(statistics);
		StringBuilder b = new StringBuilder();
		b.append(String.format(Locale.ENGLISH, "%nResult \"%s\":%n", benchmark));
		b.append(String.format(Locale.ENGLISH, "  %s ±(99.9%%) %s %s%n", format(mean), Double.isNaN(error) ? "n/a" : format(error), unit));
		b.append(String.format(Locale.ENGLISH, "  (min, avg, median, max) = (%s, %s, %s, %s), stdev = %s%n",
			format(statistics.getMin()), format(mean), format(statistics.getPercentile(50)), format(statistics.getMax()), format(statistics.getStandardDeviation())));
		if (extra!=null)
			b.append("  ").append(extra).append(String.format(Locale.ENGLISH, "%n"));
		return b.toString();
	}
}
