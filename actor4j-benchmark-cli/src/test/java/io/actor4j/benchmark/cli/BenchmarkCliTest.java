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
package io.actor4j.benchmark.cli;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.junit.Test;

import io.actor4j.benchmark.BenchmarkConfig;
import io.actor4j.benchmark.cli.sample.run.BenchmarkSampleRun;
import io.actor4j.benchmark.utils.MessageThroughputMeasurement;
import io.actor4j.benchmark.utils.TimeMeasurement;

public class BenchmarkCliTest {
	static class SampleApplication extends BenchmarkApplication {
		@Override
		public String frameworkClasspath(CommandLine line) {
			return "io.actor4j.benchmark.cli.";
		}
	}
	
	@Test
	public void test_prefix_to_name() {
		assertEquals("PingPong", BenchmarkApplication.prefixToName("ping.pong"));
		assertEquals("PingPong", BenchmarkApplication.prefixToName("ping..pong"));
		assertEquals("Ring", BenchmarkApplication.prefixToName(".ring."));
		assertEquals("", BenchmarkApplication.prefixToName(""));
	}
	
	@Test
	public void test_missing_framework_and_class() {
		assertEquals(1, new BenchmarkApplication().run(new String[] {"-class", "ping.pong"}));
		assertEquals(1, new SampleApplication().run(new String[] {}));
		assertEquals(0, new SampleApplication().run(new String[] {"-version"}));
	}
	
	@Test
	public void test_errors_give_exit_code_1() {
		assertEquals(1, new SampleApplication().run(new String[] {"-class", "no.such"}));
		assertEquals(1, new SampleApplication().run(new String[] {"-class", "sample.run", "-i", "x"}));
		assertEquals(1, new SampleApplication().run(new String[] {"-class", "sample.run", "-p", "fail=true"}));
	}
	
	@Test
	public void test_options_to_config() {
		assertEquals(0, new SampleApplication().run(new String[] {"-class", "sample.run", "-actors", "7", "-wi", "2", "-i", "3",
			"-w", "200ms", "-r", "2s", "-threads", "4", "-factor", "2", "-throughput", "50", "-mode", "yield", "-p", "number=30", "-p", "hops = 5"}));
		BenchmarkConfig config = BenchmarkSampleRun.last;
		assertEquals(7, config.numberOfActors());
		assertEquals(2, config.warmupIterations());
		assertEquals(3, config.iterations());
		assertEquals(200, config.warmupTime());
		assertEquals(2000, config.measurementTime());
		assertEquals(8, config.totalParallelism());
		assertEquals(50, config.throughput());
		assertEquals("yield", config.threadMode());
		assertEquals("30", config.param("number"));
		assertEquals(5, config.paramAsInt("hops"));
		assertEquals(1, new SampleApplication().run(new String[] {"-class", "sample.run", "-p", "novalue"}));
		assertEquals(1, new SampleApplication().run(new String[] {"-class", "sample.run", "-w", "1xs"}));
		assertEquals(1, new SampleApplication().run(new String[] {"-class", "sample.run", "-warmup", "1"})); // old name: unknown, not -w "armup"
	}
	
	@Test
	public void test_time_benchmark_quiet() {
		DescriptiveStatistics statistics = new io.actor4j.benchmark.Benchmark(new BenchmarkConfig().warmupIterations(1).iterations(3))
			.console(false)
			.start((timeMeasurement, iteration) -> {
				timeMeasurement.start();
				try { Thread.sleep(5); } catch (InterruptedException e) { }
				timeMeasurement.stop();
			});
		assertEquals(3, statistics.getN());
		assertTrue(statistics.getMin()>=4);
	}
	
	@Test
	public void test_framework_config_and_properties() {
		BenchmarkApplication application = new SampleApplication() {
			@Override
			public void frameworkOptions(org.apache.commons.cli.Options options) {
				options.addOption(org.apache.commons.cli.Option.builder("runtime").hasArg().get());
			}
			@Override
			public void frameworkConfig(CommandLine line, BenchmarkConfig config) {
				if (line.hasOption("runtime"))
					config.property("runtime", line.getOptionValue("runtime"));
			}
		};
		assertEquals(0, application.run(new String[] {"-class", "sample.run", "-runtime", "classic"}));
		assertEquals("classic", BenchmarkSampleRun.last.property("runtime"));
		assertEquals("x", BenchmarkSampleRun.last.propertyOrDefault("other", "x"));
	}
	
	
	@Test
	public void test_time_measurement() throws InterruptedException {
		TimeMeasurement measurement = new TimeMeasurement();
		assertFalse(measurement.isMeasuring());
		measurement.start();
		assertTrue(measurement.isMeasuring());
		Thread.sleep(20);
		measurement.stop();
		assertFalse(measurement.isMeasuring());
		assertTrue(measurement.getTimeMillisAsDouble()>=19);
		assertEquals(measurement.getTimeNanos()/1_000_000, measurement.getTimeMillis());
		measurement.reset();
		assertEquals(0, measurement.getTimeNanos());
	}
	
	@Test
	public void test_throughput_measurement() throws InterruptedException {
		AtomicLong counter = new AtomicLong();
		DescriptiveStatistics statistics = new DescriptiveStatistics();
		MessageThroughputMeasurement measurement = new MessageThroughputMeasurement(() -> counter.addAndGet(1000), 1, 50, 0, 50, statistics, false);
		measurement.start();
		Thread.sleep(300);
		measurement.stop();
		
		assertTrue(measurement.getMeasuredCount()>0);
		assertTrue(measurement.getSamples()>=2);
		assertEquals(measurement.getSamples(), statistics.getN());
		assertTrue(statistics.getMin()>0);
		
		// no statistics
		measurement = new MessageThroughputMeasurement(counter::get, 1, 20, 0, 20, null, false);
		measurement.start();
		Thread.sleep(100);
		measurement.stop();
		assertTrue(measurement.getSamples()>=1);
	}
	
}
