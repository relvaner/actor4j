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
package io.actor4j.benchmark.cli;

import static io.actor4j.benchmark.report.BenchmarkOutput.format;
import static org.junit.Assert.*;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.junit.Test;

import io.actor4j.benchmark.BenchmarkConfig;
import io.actor4j.benchmark.report.BenchmarkOutput;

// numbers are compared via BenchmarkOutput.format, the output follows the locale of the JVM
public class BenchmarkOutputTest {
	@Test
	public void test_header() {
		BenchmarkConfig config = new BenchmarkConfig().parallelism(4).warmupIterations(2).iterations(3).warmupTime(500).measurementTime(2000);
		config.param("number", "30").property("runtime", "test-runtime").property("benchmark", "test.Benchmark");
		config.header("Actors: %d", 400).header("Hops: %d", 5);

		String timed = BenchmarkOutput.header(config, "ignored", "Throughput", "msg/s", 4, true);
		assertTrue(timed, timed.contains("# Warmup: 2 iterations, 500 ms each"));
		assertTrue(timed, timed.contains("# Measurement: 3 iterations, 2 s each"));
		assertTrue(timed, timed.contains("# Worker threads: 4 (logical cores: "));
		assertTrue(timed, timed.contains("# Benchmark mode: Throughput, msg/s"));
		assertTrue(timed, timed.contains("# Benchmark: test.Benchmark"));
		assertTrue(timed, timed.contains("# Parameters: (runtime = test-runtime, actors = 100, mode = park, throughput = 100, number = 30)"));
		// scenario lines after the parameters, in the order they were added
		int parameters = timed.indexOf("# Parameters:");
		int actors = timed.indexOf("# Actors: 400");
		assertTrue(timed, parameters>=0 && actors>parameters && timed.indexOf("# Hops: 5")>actors);

		String untimed = BenchmarkOutput.header(config, "ignored", "Time per iteration", "ms", 4, false);
		assertTrue(untimed, untimed.contains(String.format("# Warmup: 2 iterations%n")));
		assertTrue(untimed, untimed.contains(String.format("# Measurement: 3 iterations%n")));
	}

	@Test
	public void test_iteration() {
		assertEquals(String.format("# Warmup Iteration   1: %s msg/s%n", format(1234567)), BenchmarkOutput.iteration(true, 1, 1234567, "msg/s"));
		assertEquals(String.format("Iteration  12: %s ms%n", format(1.5)), BenchmarkOutput.iteration(false, 12, 1.5, "ms"));
	}

	@Test
	public void test_result_with_median() {
		DescriptiveStatistics statistics = new DescriptiveStatistics(new double[] {1, 2, 3, 4, 100});
		String result = BenchmarkOutput.result("test.Benchmark", statistics, "ms", "forks: 2");
		assertTrue(result, result.contains("Result \"test.Benchmark\":"));
		assertTrue(result, result.contains(String.format("  %s ±(99.9%%) ", format(22))));
		// the median (3) shows the outlier, the mean (22) does not
		assertTrue(result, result.contains(String.format("(min, avg, median, max) = (%s, %s, %s, %s), stdev = ", format(1), format(22), format(3), format(100))));
		assertTrue(result, result.contains(String.format("  forks: 2%n")));
		assertFalse(result, result.contains("CI (99.9%)"));
	}

	@Test
	public void test_result_single_value() {
		String result = BenchmarkOutput.result("x", new DescriptiveStatistics(new double[] {7}), "ms", null);
		assertTrue(result, result.contains("±(99.9%) n/a ms"));
	}
}
