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
package io.actor4j.benchmark.cli.sample.fork;

import io.actor4j.benchmark.Benchmark;
import io.actor4j.benchmark.BenchmarkConfig;
import io.actor4j.benchmark.cli.BenchmarkScenario;

// small time benchmark for the fork test
public class BenchmarkFork extends BenchmarkScenario {
	public BenchmarkFork(BenchmarkConfig config) {
		super(config);
		new Benchmark(config).console(false).start((timeMeasurement, iteration) -> {
			timeMeasurement.start();
			try {
				Thread.sleep(2+iteration%3);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			timeMeasurement.stop();
		});
	}
}
