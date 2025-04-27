/*
 * Copyright (c) 2015-2020, David A. Bauer. All rights reserved.
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
	
	
	public int numberOfActors;
	
	public int warmupIterations;
	public long duration;
	
	public int parallelism;
	public int parallelismFactor; 
	
	public int throughput;
	
	public String threadMode;
	
	public String param1;
	public String param2;
		
	public BenchmarkConfig() {
		this(100);
	}
	
	public BenchmarkConfig(int numberOfActors) {
		this(numberOfActors, 10, 60_000);
	}
	
	public BenchmarkConfig(int warmupIterations, long duration) {
		this(100, warmupIterations, duration);
	}
	
	public BenchmarkConfig(int warmupIterations, long duration, String param1) {
		this(100, warmupIterations, duration, param1);
	}
	
	public BenchmarkConfig(int warmupIterations, long duration, String param1, String param2) {
		this(100, warmupIterations, duration, param1, param2);
	}
	
	public BenchmarkConfig(int numberOfActors, int warmupIterations, long duration) {
		this(numberOfActors, warmupIterations, duration, Runtime.getRuntime().availableProcessors(), 1);
	}
	
	public BenchmarkConfig(int numberOfActors, int warmupIterations, long duration, String param1) {
		this(numberOfActors, warmupIterations, duration, Runtime.getRuntime().availableProcessors(), 1, param1, null);
	}
	
	public BenchmarkConfig(int numberOfActors, int warmupIterations, long duration, String param1, String param2) {
		this(numberOfActors, warmupIterations, duration, Runtime.getRuntime().availableProcessors(), 1, param1, param2);
	}
	
	public BenchmarkConfig(int numberOfActors, int warmupIterations, long duration, int parallelism, int parallelismFactor) {
		this(numberOfActors, warmupIterations, duration, parallelism, parallelismFactor, 100, "park");
	}
	
	public BenchmarkConfig(int numberOfActors, int warmupIterations, long duration, int parallelism, int parallelismFactor, String param1) {
		this(numberOfActors, warmupIterations, duration, parallelism, parallelismFactor, 100, "park", param1, null);
	}
	
	public BenchmarkConfig(int numberOfActors, int warmupIterations, long duration, int parallelism, int parallelismFactor, String param1, String param2) {
		this(numberOfActors, warmupIterations, duration, parallelism, parallelismFactor, 100, "park", param1, param2);
	}
	
	public BenchmarkConfig(int numberOfActors, int warmupIterations, long duration, int parallelism, int parallelismFactor, int throughput, String threadMode) {
		this(numberOfActors, warmupIterations, duration, parallelism, parallelismFactor, throughput, threadMode, null, null);
	}
	
	public BenchmarkConfig(int numberOfActors, int warmupIterations, long duration, int parallelism, int parallelismFactor, int throughput, String threadMode, String param1, String param2) {
		super();
		this.numberOfActors = numberOfActors;
		
		this.warmupIterations = warmupIterations;
		this.duration = duration;
		
		this.parallelism = parallelism;
		this.parallelismFactor = parallelismFactor;
		
		this.throughput = throughput;
		
		this.threadMode = threadMode;
		
		this.param1 = param1;
		this.param2 = param2;
	}

	public int parallelism() {
		return parallelism*parallelismFactor;
	}
}
