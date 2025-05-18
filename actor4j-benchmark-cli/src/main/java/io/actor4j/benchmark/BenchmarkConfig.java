/*
 * Copyright (c) 2015-2025, David A. Bauer. All rights reserved.
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

public class BenchmarkConfig {
	private int numberOfActors;
	
	private int warmupIterations;
	private int iterations;
	
	private final long warmupTime;
	private final long measurementTime;
	
	private int parallelism;
	private int parallelismFactor; 
	
	private int throughput;
	
	private String threadMode;
	
	private String param1;
	private String param2;
		
	public BenchmarkConfig() {
		numberOfActors    = 100;
		warmupIterations  = 10;
		iterations        = 60;
		warmupTime        = 1000;
		measurementTime   = warmupTime;
		parallelism       = Runtime.getRuntime().availableProcessors();
		parallelismFactor = 1;
		throughput        = 100;
		threadMode        = "park";
		param1            = "";
		param2            = "";
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
	
	public long measurementTime() {
		return measurementTime;
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

	public String param1() {
		return param1;
	}

	public BenchmarkConfig param1(String param1) {
		this.param1 = param1;
		
		return this;
	}

	public String param2() {
		return param2;
	}

	public BenchmarkConfig param2(String param2) {
		this.param2 = param2;
		
		return this;
	}

	public int totalParallelism() {
		return parallelism*parallelismFactor;
	}
}
