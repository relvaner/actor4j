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

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import io.actor4j.benchmark.Benchmark;
import io.actor4j.benchmark.BenchmarkConfig;
import io.actor4j.benchmark.ThroughputBenchmark;
import io.actor4j.benchmark.cli.BenchmarkCliTest.SampleApplication;
import io.actor4j.benchmark.cli.sample.run.BenchmarkSampleRun;
import io.actor4j.benchmark.report.BenchmarkOutput;
import io.actor4j.benchmark.report.Json;

// time limits (-to) and the remaining failure paths of report 3
public class BenchmarkTimeoutTest {
	@Test(timeout = 10_000)
	public void test_hanging_iteration_ends_with_timeout() {
		BenchmarkConfig config = new BenchmarkConfig().warmupIterations(0).iterations(1).timeout(200);
		try {
			new Benchmark(config).console(false).start((timeMeasurement, iteration) -> {
				timeMeasurement.start();
				try {
					Thread.sleep(60_000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				timeMeasurement.stop();
			});
			fail();
		} catch (IllegalStateException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("iteration 1 did not end within 200 ms"));
			// the stack of the hanging thread shows where the scenario stands
			assertTrue(e.getMessage(), e instanceof io.actor4j.benchmark.utils.Timeouts.TimeoutException);
			assertTrue(e.getMessage(), e.getMessage().contains("Stack of the hanging thread"));
			assertTrue(e.getMessage(), e.getMessage().contains("java.lang.Thread.sleep"));
		}
	}

	@Test(timeout = 10_000)
	public void test_hanging_shutdown_ends_with_timeout() {
		BenchmarkConfig config = new BenchmarkConfig().warmupIterations(0).iterations(1).measurementTime(20).timeout(200);
		try {
			new ThroughputBenchmark(config, "test", () -> 0L).console(false).onShutdown(() -> {
				try {
					Thread.sleep(60_000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}).start();
			fail();
		} catch (IllegalStateException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("onShutdown did not end within 200 ms"));
		}
	}

	// onStop also runs after a failure of the counter (it may end parts of the scenario)
	@Test(timeout = 10_000)
	public void test_on_stop_after_counter_failure() {
		AtomicBoolean stopped = new AtomicBoolean();
		long start = System.nanoTime();
		BenchmarkConfig config = new BenchmarkConfig().warmupIterations(0).iterations(5).measurementTime(50);
		try {
			new ThroughputBenchmark(config, "test", () -> {
				if (System.nanoTime()-start>80_000_000L)
					throw new IllegalStateException("counter failed");
				return 1L;
			}).console(false).onStop(() -> stopped.set(true)).start();
			fail();
		} catch (IllegalStateException e) {
			assertTrue(stopped.get());
		}
	}

	// the fork is terminated after its time limit (here 3 s instead of the limit from -to)
	@Test(timeout = 60_000)
	public void test_hanging_fork_is_terminated() {
		long start = System.nanoTime();
		assertEquals(1, new BenchmarkForks(ForkSampleLauncher.class, "-Xmx64m", 3_000).run(new String[] {"-sample", "-class", "hang"}, 1, null));
		assertTrue((System.nanoTime()-start)/1_000_000<30_000);
	}

	// checked before the forks start a JVM
	@Test(timeout = 10_000)
	public void test_unknown_class_before_forks() {
		assertEquals(1, new ForkSampleLauncher().run(new String[] {"-sample", "-class", "no.such", "-f", "3"}));
	}

	@Test
	public void test_timeout_option_and_header() {
		assertEquals(0, new SampleApplication().run(new String[] {"-class", "sample.run", "-to", "2min"}));
		assertEquals(120_000, BenchmarkSampleRun.last.timeout());
		assertTrue(BenchmarkOutput.header(new BenchmarkConfig(), "x", "Throughput", "msg/s", 1, true).contains("# Timeout: 10 min per iteration"));
		assertEquals(1, new SampleApplication().run(new String[] {"-class", "sample.run", "-to", "x"}));
	}

	// each result file gets only its own results
	@Test
	@SuppressWarnings("unchecked")
	public void test_results_per_file() throws Exception {
		Path a = Files.createTempFile("benchmark-a", ".json");
		Path b = Files.createTempFile("benchmark-b", ".json");
		for (Path file : List.of(a, b)) {
			BenchmarkConfig config = new BenchmarkConfig().warmupIterations(0).iterations(1).resultFile(file.toString());
			config.property("benchmark", "test."+file.getFileName());
			new Benchmark(config).console(false).start((timeMeasurement, iteration) -> {
				timeMeasurement.start();
				timeMeasurement.stop();
			});
		}
		for (Path file : List.of(a, b)) {
			List<Object> results = (List<Object>)Json.parse(Files.readString(file));
			assertEquals(1, results.size());
			assertEquals("test."+file.getFileName(), ((Map<String, Object>)results.get(0)).get("benchmark"));
			Files.delete(file);
		}
	}
}
