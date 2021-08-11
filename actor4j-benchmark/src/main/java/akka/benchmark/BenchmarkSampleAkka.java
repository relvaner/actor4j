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
package akka.benchmark;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import shared.benchmark.BenchmarkConfig;
import shared.benchmark.BenchmarkSample;

public class BenchmarkSampleAkka extends BenchmarkSample {
	protected Config akkaConfig;
	
	public BenchmarkSampleAkka() {
		super();
	}

	public BenchmarkSampleAkka(BenchmarkConfig config) {
		super(config);
		
		akkaConfig = ConfigFactory.load(ConfigFactory.parseString(getDefaultConfig()));
		// Config defaultConfig = ConfigFactory.parseString(getDefaultConfig());
		// Config fallbackConfig = ConfigFactory.load("application.conf");
		// akkaConfig = ConfigFactory.load(defaultConfig.withFallback(fallbackConfig));
	}
	
	public String getDefaultConfig() {
		String newLine = System.getProperty("line.separator");
		
		return new StringBuilder()
			.append("my-dispatcher {"+newLine)
				.append("type = Dispatcher"+newLine)
				.append("executor = \"fork-join-executor\""+newLine)
				.append("fork-join-executor {"+newLine)
					.append("parallelism-min = "+config.parallelism+newLine)
					.append("parallelism-factor = "+config.parallelismFactor+newLine)
					.append("parallelism-max = "+config.parallelism+newLine)
				.append("}"+newLine)
				.append("throughput = 100"+newLine)
			.append("}"+newLine)
			.toString();
	}
}
