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
package shared.benchmark;

import static io.actor4j.core.logging.ActorLogger.*;

import io.actor4j.core.runtime.ActorThreadMode;

public class BenchmarkConfig {
	static {
		systemLogger().setLevel(ERROR); // default actor4j-core system logger
		logger().setLevel(ERROR); // default actor4j-core user logger
	}
	
	public int numberOfActors;
	
	public int warmupIterations;
	public long duration;
	
	public int parallelism;
	public int parallelismFactor; 
	
	public int throughput;
	
	public ActorThreadMode threadMode;
		
	public BenchmarkConfig() {
		this(100);
	}
	
	public BenchmarkConfig(int numberOfActors) {
		this(numberOfActors, 10, 60_000);
	}
	
	public BenchmarkConfig(int warmupIterations, long duration) {
		this(100, warmupIterations, duration);
	}
	
	public BenchmarkConfig(int numberOfActors, int warmupIterations, long duration) {
		this(numberOfActors, warmupIterations, duration, Runtime.getRuntime().availableProcessors(), 1);
	}
	
	public BenchmarkConfig(int numberOfActors, int warmupIterations, long duration, int parallelism, int parallelismFactor) {
		this(numberOfActors, warmupIterations, duration, parallelism, parallelismFactor, 100, ActorThreadMode.SLEEP);
	}
	
	public BenchmarkConfig(int numberOfActors, int warmupIterations, long duration, int parallelism, int parallelismFactor, int throughput, ActorThreadMode threadMode) {
		super();
		this.numberOfActors = numberOfActors;
		
		this.warmupIterations = warmupIterations;
		this.duration = duration;
		
		this.parallelism = parallelism;
		this.parallelismFactor = parallelismFactor;
		
		this.throughput = throughput;
		
		this.threadMode = threadMode;
	}

	public int parallelism() {
		return parallelism*parallelismFactor;
	}
}
