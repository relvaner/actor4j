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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BenchmarkConfig {
	// default duration of a warmup and a measurement iteration in ms (-w, -r)
	public static final long DEFAULT_TIME    = 1_000;
	// default timeout of an iteration in ms (-to), like JMH
	public static final long DEFAULT_TIMEOUT = 600_000;
	
	private int numberOfActors;
	
	private int warmupIterations;
	private int iterations;
	
	private long warmupTime;
	private long measurementTime;
	// limit of an iteration (time scenarios), of onStop/onShutdown and of a fork per iteration, in ms
	private long timeout;
	
	private int parallelism;
	private int parallelismFactor; 
	
	private int throughput;
	
	private String threadMode;
	
	// parameters of the scenario, like JMH's -p name=value (e.g. number, hops)
	private final Map<String, String> params;
	
	// result file in the JSON format of JMH (-rff), null = none
	private String resultFile;
	
	// framework specific settings (e.g. the runtime), set by the launcher via frameworkConfig
	private final Map<String, String> properties;

	// scenario information (e.g. number of actors, hops), printed as # lines at the end of the header
	private final List<String> header;
		
	public BenchmarkConfig() {
		numberOfActors    = 100;
		warmupIterations  = 10;
		iterations        = 60;
		warmupTime        = DEFAULT_TIME;
		measurementTime   = warmupTime;
		timeout           = DEFAULT_TIMEOUT;
		parallelism       = Runtime.getRuntime().availableProcessors();
		parallelismFactor = 1;
		throughput        = 100;
		threadMode        = "park";
		params            = new LinkedHashMap<>();
		properties        = new HashMap<>();
		header            = new ArrayList<>();
	}
	
	public int numberOfActors() {
		return numberOfActors;
	}

	public BenchmarkConfig numberOfActors(int numberOfActors) {
		this.numberOfActors = numberOfActors;
		
		return this;
	}

	public int warmupIterations() {
		return warmupIterations;
	}

	public BenchmarkConfig warmupIterations(int warmupIterations) {
		this.warmupIterations = warmupIterations;
		
		return this;
	}
	
	public int iterations() {
		return iterations ;
	}

	public BenchmarkConfig iterations(int iterations) {
		this.iterations = iterations;
		
		return this;
	}
	
	public long warmupTime() {
		return warmupTime;
	}
	
	public BenchmarkConfig warmupTime(long warmupTime) {
		this.warmupTime = warmupTime;
		
		return this;
	}
	
	public long measurementTime() {
		return measurementTime;
	}
	
	public BenchmarkConfig measurementTime(long measurementTime) {
		this.measurementTime = measurementTime;
		
		return this;
	}

	public long timeout() {
		return timeout;
	}
	
	public BenchmarkConfig timeout(long timeout) {
		this.timeout = timeout;
		
		return this;
	}
	
	public int parallelism() {
		return parallelism;
	}

	public BenchmarkConfig parallelism(int parallelism) {
		this.parallelism = parallelism;
		
		return this;
	}

	public int parallelismFactor() {
		return parallelismFactor;
	}

	public BenchmarkConfig parallelismFactor(int parallelismFactor) {
		this.parallelismFactor = parallelismFactor;
		
		return this;
	}

	public int throughput() {
		return throughput;
	}

	public BenchmarkConfig throughput(int throughput) {
		this.throughput = throughput;
		
		return this;
	}

	public String threadMode() {
		return threadMode;
	}

	public BenchmarkConfig threadMode(String threadMode) {
		this.threadMode = threadMode;
		
		return this;
	}

	public BenchmarkConfig param(String name, String value) {
		params.put(name, value);
		
		return this;
	}
	
	public String param(String name) {
		return params.get(name);
	}
	
	public String paramOrDefault(String name, String defaultValue) {
		return params.getOrDefault(name, defaultValue);
	}
	
	public Map<String, String> params() {
		return Collections.unmodifiableMap(params);
	}
	
	// for required numeric parameters: clear message instead of a NumberFormatException in the middle of a run
	public int paramAsInt(String name) {
		String value = params.get(name);
		try {
			return Integer.parseInt(value.trim());
		} catch (RuntimeException e) {
			throw new IllegalArgumentException(String.format("Missing or invalid -p %s=<number>: '%s'", name, value==null ? "" : value));
		}
	}
	
	public int paramAsInt(String name, int defaultValue) {
		return params.containsKey(name) ? paramAsInt(name) : defaultValue;
	}
	
	// time like JMH (-w, -r): a number with unit ns, us, ms, s, min, h (without unit: s), here in ms (at least 1 ms)
	public static long parseTime(String value) {
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\s*(\\d+)\\s*(ns|us|ms|s|min|m|h)?\\s*").matcher(value);
		if (!m.matches())
			throw new IllegalArgumentException("Invalid time '"+value+"' (e.g. 1s, 500ms, 2min)");
		long amount = Long.parseLong(m.group(1));
		String unit = m.group(2)==null ? "s" : m.group(2);
		double millis = switch (unit) {
			case "ns" -> amount/1e6;
			case "us" -> amount/1e3;
			case "ms" -> amount;
			case "s" -> amount*1e3;
			case "min", "m" -> amount*60e3;
			default -> amount*3600e3;
		};
		if (millis<1)
			throw new IllegalArgumentException("Time '"+value+"' below 1 ms is not supported");
		return Math.round(millis);
	}

	public String resultFile() {
		return resultFile;
	}
	
	public BenchmarkConfig resultFile(String resultFile) {
		this.resultFile = resultFile;
		
		return this;
	}
	
	public String property(String key) {
		return properties.get(key);
	}
	
	public String propertyOrDefault(String key, String defaultValue) {
		return properties.getOrDefault(key, defaultValue);
	}
	
	public BenchmarkConfig property(String key, String value) {
		properties.put(key, value);
		
		return this;
	}
	
	// adds a line to the header, e.g. header("Actors: %d", n) gives "# Actors: n"
	public BenchmarkConfig header(String format, Object... args) {
		header.add(String.format(format, args));

		return this;
	}

	public List<String> header() {
		return Collections.unmodifiableList(header);
	}

	// iterations of a run: at least one measured iteration, no negative warmup
	public void validateIterations() {
		if (iterations<1)
			throw new IllegalArgumentException("Iterations (-i) must be at least 1: "+iterations);
		if (warmupIterations<0)
			throw new IllegalArgumentException("Warmup iterations (-wi) must not be negative: "+warmupIterations);
	}
	
	public int totalParallelism() {
		return parallelism*parallelismFactor;
	}
}
