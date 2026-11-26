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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import io.actor4j.benchmark.BenchmarkConfig;

// Result of a run in the JSON format of JMH (-rf json -rff file), so that CLI and JMH results can be evaluated
// with the same tools. All results of a JVM are written to the file as one list (rewritten after each result).
public final class BenchmarkResult {
	public static final String MODE_THROUGHPUT   = "thrpt";
	public static final String MODE_TIME       = "time"; // one run per iteration, no JMH mode

	protected static final double[] PERCENTILES = { 0.0, 50.0, 90.0, 95.0, 99.0, 99.9, 99.99, 99.999, 99.9999, 100.0 };

	// results of this JVM per result file (absolute path), each file gets only its own results
	private static final Map<String, List<Map<String, Object>>> RESULTS = new LinkedHashMap<>();

	private BenchmarkResult() {
	}

	// writes the result if a result file is configured (-rff), name is the fallback for the benchmark name
	public static synchronized void record(BenchmarkConfig config, String name, String mode, String unit,
			DescriptiveStatistics statistics, Map<String, Object[]> secondary) {
		if (config.resultFile()==null)
			return;

		Path file = Path.of(config.resultFile()).toAbsolutePath();
		List<Map<String, Object>> results = RESULTS.computeIfAbsent(file.toString(), k -> new ArrayList<>());
		results.add(create(config, name, mode, unit, statistics, secondary));
		try {
			Files.writeString(file, Json.write(results), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static Map<String, Object> create(BenchmarkConfig config, String name, String mode, String unit,
			DescriptiveStatistics statistics, Map<String, Object[]> secondary) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("jmhVersion", "actor4j-benchmark-cli");
		result.put("benchmark", config.propertyOrDefault("benchmark", name));
		result.put("mode", mode);
		result.put("threads", 1); // JMH: calling threads, here one. The worker threads are in params (parallelism, factor)
		result.put("forks", 1);
		result.put("jvm", Path.of(System.getProperty("java.home"), "bin", "java").toString());
		result.put("jvmArgs", new ArrayList<Object>(ManagementFactory.getRuntimeMXBean().getInputArguments()));
		result.put("jdkVersion", System.getProperty("java.version"));
		result.put("vmName", System.getProperty("java.vm.name"));
		result.put("vmVersion", System.getProperty("java.vm.version"));
		result.put("warmupIterations", config.warmupIterations());
		result.put("warmupTime", config.warmupTime()+" ms");
		result.put("measurementIterations", config.iterations());
		result.put("measurementTime", config.measurementTime()+" ms");

		// same names as the parameters of the JMH benchmarks (runtime, actors, mode = thread mode), "benchmark" names the scenario
		Map<String, Object> params = new LinkedHashMap<>();
		// short name of the scenario (-class, e.g. fib.with.stopping)
		// so that results of the frameworks can be grouped by scenario and runtime
		if (config.property("scenario")!=null)
			params.put("scenario", config.property("scenario"));
		params.put("runtime", config.propertyOrDefault("runtime", "n/a"));
		// pinning of the actor threads (-affinity of a launcher), so that pinned and unpinned results stay apart
		if (config.property("affinity")!=null)
			params.put("affinity", config.property("affinity"));
		// the shared pool of the hybrid runtime (-poolAffinity, default: the rest of -affinity)
		if (config.property("poolAffinity")!=null)
			params.put("poolAffinity", config.property("poolAffinity"));
		params.put("actors", String.valueOf(config.numberOfActors()));
		params.put("parallelism", String.valueOf(config.parallelism()));
		params.put("factor", String.valueOf(config.parallelismFactor()));
		params.put("throughput", String.valueOf(config.throughput()));
		params.put("mode", config.threadMode());
		params.putAll(config.params());
		result.put("params", params);

		result.put("primaryMetric", primaryMetric(statistics, unit));

		Map<String, Object> secondaryMetrics = new LinkedHashMap<>();
		if (secondary!=null)
			for (Map.Entry<String, Object[]> e : secondary.entrySet()) {
				Map<String, Object> metric = new LinkedHashMap<>();
				metric.put("score", e.getValue()[0]);
				metric.put("scoreUnit", e.getValue()[1]);
				secondaryMetrics.put(e.getKey(), metric);
			}
		result.put("secondaryMetrics", secondaryMetrics);

		return result;
	}

	protected static Map<String, Object> primaryMetric(DescriptiveStatistics statistics, String unit) {
		List<Object> raw = new ArrayList<>();
		Arrays.stream(statistics.getValues()).forEach(raw::add);
		return primaryMetric(statistics, unit, List.of(raw));
	}
	
	// rawData: one list of measured values per fork
	protected static Map<String, Object> primaryMetric(DescriptiveStatistics statistics, String unit, List<Object> rawData) {
		double mean = statistics.getMean();
		double error = scoreError(statistics);

		Map<String, Object> metric = new LinkedHashMap<>();
		metric.put("score", mean);
		metric.put("scoreError", error);
		metric.put("scoreConfidence", List.of(mean-error, mean+error));
		Map<String, Object> percentiles = new LinkedHashMap<>();
		for (double p : PERCENTILES)
			percentiles.put(String.valueOf(p), statistics.getN()==0 ? Double.NaN : p==0.0 ? statistics.getMin() : statistics.getPercentile(p));
		metric.put("scorePercentiles", percentiles);
		metric.put("scoreUnit", unit);
		metric.put("rawData", rawData);

		return metric;
	}

	// one result from the results of the forks (one result per fork, same benchmark): like JMH, score and error
	// over the measured values of all forks, rawData with one list per fork, secondary metrics as mean of the forks
	// (count: messages per fork on average, not the sum over all forks)
	@SuppressWarnings("unchecked")
	public static Map<String, Object> merge(List<Map<String, Object>> forkResults) {
		if (forkResults.isEmpty())
			throw new IllegalArgumentException("no fork results");
		Map<String, Object> result = new LinkedHashMap<>(forkResults.get(0));
		result.put("forks", forkResults.size());
		
		DescriptiveStatistics statistics = new DescriptiveStatistics();
		List<Object> rawData = new ArrayList<>();
		Map<String, DescriptiveStatistics> secondary = new LinkedHashMap<>();
		Map<String, Object> secondaryUnits = new LinkedHashMap<>();
		for (Map<String, Object> fork : forkResults) {
			Map<String, Object> metric = (Map<String, Object>)fork.get("primaryMetric");
			for (Object values : (List<Object>)metric.get("rawData")) {
				rawData.add(values);
				for (Object value : (List<Object>)values)
					statistics.addValue(Json.number(value));
			}
			if (fork.get("secondaryMetrics") instanceof Map<?, ?> metrics)
				for (Map.Entry<?, ?> e : metrics.entrySet()) {
					Map<String, Object> m = (Map<String, Object>)e.getValue();
					secondary.computeIfAbsent(String.valueOf(e.getKey()), k -> new DescriptiveStatistics()).addValue(Json.number(m.get("score")));
					secondaryUnits.put(String.valueOf(e.getKey()), m.get("scoreUnit"));
				}
		}
		String unit = String.valueOf(((Map<String, Object>)forkResults.get(0).get("primaryMetric")).get("scoreUnit"));
		result.put("primaryMetric", primaryMetric(statistics, unit, rawData));
		
		Map<String, Object> secondaryMetrics = new LinkedHashMap<>();
		for (Map.Entry<String, DescriptiveStatistics> e : secondary.entrySet()) {
			Map<String, Object> metric = new LinkedHashMap<>();
			metric.put("score", e.getValue().getMean());
			metric.put("scoreUnit", secondaryUnits.get(e.getKey()));
			secondaryMetrics.put(e.getKey(), metric);
		}
		result.put("secondaryMetrics", secondaryMetrics);
		
		return result;
	}
	
	// half width of the 99.9 % confidence interval (Student's t), like JMH, NaN below two values
	public static double scoreError(DescriptiveStatistics statistics) {
		long n = statistics.getN();
		if (n<2)
			return Double.NaN;
		double t = new TDistribution(n-1).inverseCumulativeProbability(1-0.001/2);
		return t*statistics.getStandardDeviation()/Math.sqrt(n);
	}

	// for tests: results of this JVM for a result file
	static synchronized List<Map<String, Object>> results(String resultFile) {
		return RESULTS.getOrDefault(Path.of(resultFile).toAbsolutePath().toString(), List.of());
	}
}
