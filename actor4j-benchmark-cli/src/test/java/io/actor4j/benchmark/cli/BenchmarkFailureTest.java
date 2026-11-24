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
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.junit.Test;

import io.actor4j.benchmark.Benchmark;
import io.actor4j.benchmark.BenchmarkConfig;
import io.actor4j.benchmark.ThroughputBenchmark;
import io.actor4j.benchmark.cli.BenchmarkCliTest.SampleApplication;
import io.actor4j.benchmark.cli.sample.run.BenchmarkSampleRun;
import io.actor4j.benchmark.utils.TimeMeasurement;

// failures and wrong input: clear error and exit code 1 instead of a hang, a silent result or a late failure
public class BenchmarkFailureTest {
	// a failing counter ends the run with the failure (before: the measurement waited forever)
	@Test(timeout = 10_000)
	public void test_counter_failure_does_not_hang() {
		AtomicBoolean shutdown = new AtomicBoolean();
		long start = System.nanoTime();
		BenchmarkConfig config = new BenchmarkConfig().warmupIterations(0).iterations(5).measurementTime(50);
		try {
			new ThroughputBenchmark(config, "test", () -> {
				if (System.nanoTime()-start>80_000_000L)
					throw new IllegalStateException("counter failed");
				return 1L;
			}).console(false).onShutdown(() -> shutdown.set(true)).start();
			fail();
		} catch (IllegalStateException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("counter failed"));
			assertTrue(shutdown.get());
		}
	}

	@Test
	public void test_invalid_numbers_give_exit_code_1() {
		String[][] invalid = {
			{"-threads", "0"}, {"-factor", "0"}, {"-actors", "0"}, {"-i", "0"}, {"-i", "-1"},
			{"-wi", "-1"}, {"-throughput", "0"}, {"-f", "-1"}, {"-threads", "x"}};
		for (String[] option : invalid) {
			BenchmarkSampleRun.last = null;
			assertEquals(String.join(" ", option), 1, new SampleApplication().run(new String[] {"-class", "sample.run", option[0], option[1]}));
			assertNull(String.join(" ", option), BenchmarkSampleRun.last); // the scenario did not run
		}
		// lower bounds themselves are fine
		assertEquals(0, new SampleApplication().run(new String[] {"-class", "sample.run", "-wi", "0", "-i", "1", "-f", "0", "-threads", "1"}));
	}

	@Test(expected = IllegalArgumentException.class)
	public void test_time_benchmark_requires_iterations() {
		new Benchmark(new BenchmarkConfig().iterations(0)).console(false).start((timeMeasurement, iteration) -> {
			timeMeasurement.start();
			timeMeasurement.stop();
		});
	}

	// a forgotten stop() or start() would give 0 ms or a wrong time
	@Test
	public void test_time_measurement_must_be_complete() {
		BenchmarkConfig config = new BenchmarkConfig().warmupIterations(0).iterations(1);
		try {
			new Benchmark(config).console(false).start((timeMeasurement, iteration) -> timeMeasurement.start());
			fail();
		} catch (IllegalStateException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("without stop()"));
		}
		try {
			new Benchmark(config).console(false).start((timeMeasurement, iteration) -> { });
			fail();
		} catch (IllegalStateException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("without start() and stop()"));
		}
		try {
			new TimeMeasurement().stop();
			fail();
		} catch (IllegalStateException e) {
			// expected
		}
	}

	// before: "Invalid arguments: JSON: empty"
	@Test
	public void test_fork_without_result() {
		assertEquals(1, new ForkSampleLauncher().run(new String[] {"-sample", "-class", "empty", "-f", "1"}));
	}

	// checked before the run, not at its end
	@Test
	public void test_result_file_not_writable() throws Exception {
		Path directory = Files.createTempDirectory("benchmark-result");
		String missing = directory.resolve("no-such-directory").resolve("result.json").toString();
		BenchmarkSampleRun.last = null;
		assertEquals(1, new SampleApplication().run(new String[] {"-class", "sample.run", "-rff", missing}));
		assertNull(BenchmarkSampleRun.last);
		assertEquals(1, new SampleApplication().run(new String[] {"-class", "sample.run", "-rff", directory.toString()})); // a directory
		assertEquals(0, new SampleApplication().run(new String[] {"-class", "sample.run", "-rff", directory.resolve("result.json").toString()}));
		Files.delete(directory);
	}

	@Test
	public void test_option_with_equals() {
		assertEquals(0, new SampleApplication().run(new String[] {"-class", "sample.run", "-wi=3", "-i=2"}));
		assertEquals(3, BenchmarkSampleRun.last.warmupIterations());
		assertEquals(2, BenchmarkSampleRun.last.iterations());
		assertEquals(1, new SampleApplication().run(new String[] {"-class", "sample.run", "-warmup=3"}));
	}

	@Test
	public void test_unknown_option_skips_values() {
		Options options = new Options();
		options.addOption(Option.builder("jvmArgs").hasArg().get());
		options.addOption(Option.builder("i").hasArg().get());
		assertNull(BenchmarkApplication.unknownOption(options, new String[] {"-jvmArgs", "-Xmx1g", "-i", "1"}));
		assertEquals("-x", BenchmarkApplication.unknownOption(options, new String[] {"-i", "1", "-x"}));
	}

	@Test
	public void test_jvm_args_with_quotes() {
		assertEquals(List.of("-Xmx1g", "-Dpath=C:\\Program Files", "-Dempty="),
			BenchmarkForks.splitArgs("  -Xmx1g   -Dpath=\"C:\\Program Files\" -Dempty=\"\" "));
		try {
			BenchmarkForks.splitArgs("-Dpath=\"C:\\Program Files");
			fail();
		} catch (IllegalArgumentException e) {
			// expected
		}
	}

	@Test
	public void test_fork_command() {
		assertTrue(BenchmarkForks.isDebugAgent("-agentlib:jdwp=transport=dt_socket,server=y,address=5005"));
		assertFalse(BenchmarkForks.isDebugAgent("-Xmx1g"));

		List<String> command = new BenchmarkForks(ForkSampleLauncher.class, "-Xmx64m")
			.command(new String[] {"-sample", "-f=2", "-rf", "json", "-jvmArgs", "-Xmx1g", "-class", "fork"}, Path.of("fork.json"));
		assertTrue(command.contains("-Xmx64m"));
		assertFalse(command.contains("-f=2")); // otherwise each fork would start forks again
		assertFalse(command.contains("-Xmx1g"));
		assertEquals(List.of("-sample", "-class", "fork", "-rff", "fork.json"), command.subList(command.size()-5, command.size()));
	}
}
