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
package other.benchmark.samples.dictionary;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import shared.benchmark.Benchmark;
import shared.benchmark.BenchmarkConfig;
import shared.benchmark.BenchmarkSample;

// @see https://hackernoon.com/what-to-do-with-5-000-000-akka-actors-381a915a0f78
public class BenchmarkDictionary extends BenchmarkSample {
	public BenchmarkDictionary(BenchmarkConfig config) {
		super();
		
		int attempts = 2_500_000;
		int keySize  = 5_000_000;
		
		Map<Integer, Integer> dictionary = new HashMap<>();
		Random random = new Random(); 
		
		Benchmark benchmark = new Benchmark(config);
		
		benchmark.start((timeMeasurement, iteration) -> {
			timeMeasurement.start();
			
			for (int i=0; i<attempts; i++)
				dictionary.put(random.nextInt(keySize), random.nextInt());
			for (int i=0; i<keySize; i++)
				dictionary.get(random.nextInt(keySize));
			
			timeMeasurement.stop();
		});
	}
	
	public static void main(String[] args) {
		new BenchmarkDictionary(new BenchmarkConfig(10, 60));
	}
}
