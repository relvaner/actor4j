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

public class BenchmarkConfig {
	static {
		systemLogger().setLevel(ERROR); // default actor4j-core system logger
		logger().setLevel(ERROR); // default actor4j-core user logger
	}
	
	public int numberOfActors;
	
	public int warmupIterations;
	public long durationTimes;
	
	public int parallelismMin;
	public int parallelismFactor; 
		
	public BenchmarkConfig() {
		this(100);
	}
	
	public BenchmarkConfig(int numberOfActors) {
		this(numberOfActors, 10, 60_000);
	}
	
	public BenchmarkConfig(int warmupIterations, long durationTimes) {
		this(100, warmupIterations, durationTimes);
	}
	
	public BenchmarkConfig(int numberOfActors, int warmupIterations, long durationTimes) {
		super();
		this.numberOfActors = numberOfActors;
		
		this.warmupIterations = warmupIterations;
		this.durationTimes = durationTimes;
		
		parallelismMin = Runtime.getRuntime().availableProcessors();
		parallelismFactor = 1;
	}

	public int parallelism() {
		return parallelismMin*parallelismFactor;
	}
	
	public long getDuration() {
		return durationTimes;
	}
	
	public long getTimes() {
		return durationTimes;
	}
}
