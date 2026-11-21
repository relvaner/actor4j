package io.actor4j.benchmark.cli.sample.run;

import io.actor4j.benchmark.BenchmarkConfig;
import io.actor4j.benchmark.cli.BenchmarkScenario;

public class BenchmarkSampleRun extends BenchmarkScenario {
	public static BenchmarkConfig last;
	
	public BenchmarkSampleRun(BenchmarkConfig config) {
		super(config);
		if (config.param1().equals("fail"))
			throw new IllegalStateException("failed");
		last = config;
	}
}
