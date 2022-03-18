/*
 * Copyright (c) 2015-2022, David A. Bauer. All rights reserved.
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
package actor4j.benchmark;

import io.actor4j.core.ActorSystem;
import io.actor4j.core.config.ActorSystemConfig;
import io.actor4j.core.config.ActorSystemConfig.Builder;
import shared.benchmark.BenchmarkConfig;
import shared.benchmark.BenchmarkSample;

public class BenchmarkSampleActor4j extends BenchmarkSample {
	public BenchmarkSampleActor4j(BenchmarkConfig config) {
		super(config);
	}
	
	public ActorSystem createActorSystem(String name) {
		//io.actor4j.analyzer.config.ActorAnalyzerConfig.Builder<?> builder = io.actor4j.analyzer.config.ActorAnalyzerConfig.builder();
		Builder<?> builder = ActorSystemConfig.builder();
		
		if (config.parallelism>0)
			builder.parallelism(config.parallelism);
		if (config.parallelismFactor>0)
			builder.parallelismFactor(config.parallelismFactor);
		
		builder.name(name);
		builder.sleepMode();
		builder.counterEnabled(true);
		
		builder.horizontalPodAutoscalerSyncTime(Integer.MAX_VALUE); // disabled
		builder.watchdogSyncTime(Integer.MAX_VALUE); // disabled
		
		//return new io.actor4j.analyzer.ActorAnalyzer(new io.actor4j.analyzer.DefaultActorAnalyzerThread(2000, true, true, true), builder.build());
		return new ActorSystem(builder.build());
	}
}
