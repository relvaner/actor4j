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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.junit.Test;

import io.actor4j.benchmark.Benchmark;
import io.actor4j.benchmark.BenchmarkConfig;
import io.actor4j.benchmark.ThroughputBenchmark;
import io.actor4j.benchmark.report.BenchmarkResult;
import io.actor4j.benchmark.report.Json;

public class BenchmarkResultTest {
	@Test
	@SuppressWarnings("unchecked")
	public void test_json_round_trip() {
		Map<String, Object> value = new LinkedHashMap<>();
		value.put("text", "a \"quoted\" \\ line\nnext");
		value.put("number", 1.5);
		value.put("long", 42L);
		value.put("nan", Double.NaN);
		value.put("list", List.of(1.0, 2.0));
		value.put("nested", List.of(Map.of("k", true)));
		value.put("empty", Map.of());

		Map<String, Object> parsed = (Map<String, Object>)Json.parse(Json.write(value));
		assertEquals("a \"quoted\" \\ line\nnext", parsed.get("text"));
		assertEquals(1.5, Json.number(parsed.get("number")), 0);
		assertEquals(42, Json.number(parsed.get("long")), 0);
		assertTrue(Double.isNaN(Json.number(parsed.get("nan"))));
		assertEquals(List.of(1.0, 2.0), parsed.get("list"));
		assertEquals(Boolean.TRUE, ((Map<String, Object>)((List<Object>)parsed.get("nested")).get(0)).get("k"));
		assertEquals(Map.of(), parsed.get("empty"));
	}

	@Test
	public void test_score_error_like_jmh() {
		DescriptiveStatistics statistics = new DescriptiveStatistics(new double[] {1, 2, 3, 4, 5});
		// t(0.9995, 4) = 8.6103, sd = 1.5811, n = 5
		assertEquals(8.6103*1.5811/Math.sqrt(5), BenchmarkResult.scoreError(statistics), 0.01);
		assertTrue(Double.isNaN(BenchmarkResult.scoreError(new DescriptiveStatistics(new double[] {1}))));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void test_result_files_of_both_benchmarks() throws Exception {
		Path file = Files.createTempFile("benchmark-result", ".json");
		BenchmarkConfig config = new BenchmarkConfig().warmupIterations(1).warmupTime(20).iterations(3).measurementTime(20).resultFile(file.toString());
		config.property("benchmark", "test.Throughput").property("runtime", "test-runtime");
		long start = System.nanoTime();
		new ThroughputBenchmark(config, "throughput", () -> (System.nanoTime()-start)/1000).console(false).start();

		List<Object> results = (List<Object>)Json.parse(Files.readString(file));
		Map<String, Object> result = (Map<String, Object>)results.get(results.size()-1);
		assertEquals("test.Throughput", result.get("benchmark"));
		assertEquals("thrpt", result.get("mode"));
		// JMH: calling threads, the worker threads are in params
		assertEquals(1, Json.number(result.get("threads")), 0);
		assertEquals(String.valueOf(config.parallelism()), ((Map<String, Object>)result.get("params")).get("parallelism"));
		assertEquals("test-runtime", ((Map<String, Object>)result.get("params")).get("runtime"));
		assertFalse(((Map<String, Object>)result.get("params")).containsKey("name")); // the name is in "benchmark"
		assertFalse(((Map<String, Object>)result.get("params")).containsKey("scenario")); // only with -class
		Map<String, Object> metric = (Map<String, Object>)result.get("primaryMetric");
		assertEquals("msg/s", metric.get("scoreUnit"));
		assertEquals(3, ((List<Object>)((List<Object>)metric.get("rawData")).get(0)).size());
		assertTrue(((Map<String, Object>)result.get("secondaryMetrics")).containsKey("derived"));

		BenchmarkConfig timeConfig = new BenchmarkConfig().warmupIterations(1).iterations(2).resultFile(file.toString());
		timeConfig.property("benchmark", "test.Time");
		timeConfig.property("scenario", "time.test");
		new Benchmark(timeConfig).console(false).start((timeMeasurement, iteration) -> {
			timeMeasurement.start();
			timeMeasurement.stop();
		});
		results = (List<Object>)Json.parse(Files.readString(file));
		result = (Map<String, Object>)results.get(results.size()-1);
		assertEquals("test.Time", result.get("benchmark"));
		assertEquals("time", result.get("mode"));
		assertEquals("time.test", ((Map<String, Object>)result.get("params")).get("scenario"));
		assertEquals("ms", ((Map<String, Object>)result.get("primaryMetric")).get("scoreUnit"));
		// both results of this JVM are in the file
		assertEquals("test.Throughput", ((Map<String, Object>)results.get(results.size()-2)).get("benchmark"));
		Files.delete(file);
	}


	// 2 forks, one JVM after the other: one merged result with the values of both forks
	@Test
	@SuppressWarnings("unchecked")
	public void test_forks_merged_like_jmh() throws Exception {
		Path file = Files.createTempFile("benchmark-forks", ".json");
		int exitCode = new ForkSampleLauncher().run(new String[] {"-sample", "-class", "fork", "-f", "2", "-jvmArgs", "-Xmx256m -Dfork.test=1", "-wi", "1", "-i", "3", "-rff", file.toString()});
		assertEquals(0, exitCode);
		
		List<Object> results = (List<Object>)Json.parse(Files.readString(file));
		assertEquals(1, results.size());
		Map<String, Object> result = (Map<String, Object>)results.get(0);
		assertEquals("io.actor4j.benchmark.cli.sample.fork.BenchmarkFork", result.get("benchmark"));
		assertEquals(2, (int)Json.number(result.get("forks")));
		// the forks ran with -jvmArgs, from Java 22 on also with native access (FFM, e.g. thread pinning)
		assertEquals(Runtime.version().feature()>=22 ? List.of("-Xmx256m", "-Dfork.test=1", "--enable-native-access=ALL-UNNAMED") : List.of("-Xmx256m", "-Dfork.test=1"), result.get("jvmArgs"));
		Map<String, Object> metric = (Map<String, Object>)result.get("primaryMetric");
		List<Object> rawData = (List<Object>)metric.get("rawData");
		assertEquals(2, rawData.size());
		assertEquals(3, ((List<Object>)rawData.get(0)).size());
		assertFalse(Double.isNaN(Json.number(metric.get("scoreError"))));
		Files.delete(file);
	}
	
	@Test
	public void test_merge_of_fork_results() {
		Map<String, Object> fork1 = BenchmarkResult.create(new BenchmarkConfig(), "x", BenchmarkResult.MODE_TIME, "ms", new DescriptiveStatistics(new double[] {1, 2}), null);
		Map<String, Object> fork2 = BenchmarkResult.create(new BenchmarkConfig(), "x", BenchmarkResult.MODE_TIME, "ms", new DescriptiveStatistics(new double[] {3, 4}), null);
		Map<String, Object> merged = BenchmarkResult.merge(List.of(fork1, fork2));
		assertEquals(2, merged.get("forks"));
		@SuppressWarnings("unchecked")
		Map<String, Object> metric = (Map<String, Object>)merged.get("primaryMetric");
		assertEquals(2.5, Json.number(metric.get("score")), 1e-9);
		assertEquals(BenchmarkResult.scoreError(new DescriptiveStatistics(new double[] {1, 2, 3, 4})), Json.number(metric.get("scoreError")), 1e-9);
	}
	
	@Test
	public void test_unknown_result_format() {
		assertEquals(1, new BenchmarkCliTest.SampleApplication().run(new String[] {"-class", "sample.run", "-rf", "csv"}));
	}
}
