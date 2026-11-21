package io.actor4j.benchmark.cli;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.junit.Test;

import io.actor4j.benchmark.BenchmarkConfig;
import io.actor4j.benchmark.cli.sample.run.BenchmarkSampleRun;
import io.actor4j.benchmark.report.BenchmarkReportAnalyzer;
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
		assertEquals(1, new SampleApplication().run(new String[] {"-class", "sample.run", "-iters", "x"}));
		assertEquals(1, new SampleApplication().run(new String[] {"-class", "sample.run", "-param1", "fail"}));
	}
	
	@Test
	public void test_options_to_config() {
		assertEquals(0, new SampleApplication().run(new String[] {"-class", "sample.run", "-actors", "7", "-warmup", "2", "-iters", "3",
			"-warmupTime", "200", "-time", "500", "-threads", "4", "-factor", "2", "-throughput", "50", "-mode", "yield", "-param1", "a", "-param2", "b"}));
		BenchmarkConfig config = BenchmarkSampleRun.last;
		assertEquals(7, config.numberOfActors());
		assertEquals(2, config.warmupIterations());
		assertEquals(3, config.iterations());
		assertEquals(200, config.warmupTime());
		assertEquals(500, config.measurementTime());
		assertEquals(8, config.totalParallelism());
		assertEquals(50, config.throughput());
		assertEquals("yield", config.threadMode());
		assertEquals("a", config.param1());
		assertEquals("b", config.param2());
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
		AtomicLong warmupCounter = new AtomicLong(-1);
		DescriptiveStatistics statistics = new DescriptiveStatistics();
		MessageThroughputMeasurement measurement = new MessageThroughputMeasurement(() -> counter.addAndGet(1000), 1, warmupCounter, statistics, false, 50);
		measurement.start();
		Thread.sleep(300);
		measurement.stop();
		
		assertTrue(warmupCounter.get()>0);
		assertTrue(measurement.getSamples()>=2);
		assertEquals(measurement.getSamples(), statistics.getN());
		assertTrue(statistics.getMin()>0);
		
		// no warmup counter and no statistics
		measurement = new MessageThroughputMeasurement(counter::get, 1, null, null, false, 20);
		measurement.start();
		Thread.sleep(100);
		measurement.stop();
		assertTrue(measurement.getSamples()>=1);
	}
	
	@Test
	public void test_report_analyzer_groups_by_header() throws IOException {
		DecimalFormat format = new DecimalFormat("###,###,###,##0.###");
		List<String> lines = new ArrayList<>();
		for (double[] run : new double[][] {{1000.5, 10}, {3000.5, 30}}) {
			lines.add("#actors: 100");
			lines.add("activeThreads: 4");
			lines.add("Benchmark started (PingPong)...");
			lines.add("statistics::mean::derived : 1 msg/s");
			lines.add("statistics::mean          : "+format.format(run[0])+" msg/s");
			lines.add("#actors: 200");
			lines.add("activeThreads: 4");
			lines.add("Benchmark started (PingPong)...");
			lines.add("statistics::mean          : "+format.format(run[1])+" msg/s");
		}
		StringWriter output = new StringWriter();
		BenchmarkReportAnalyzer.analyze(new StringReader(String.join("\n", lines)), output);
		
		String result = output.toString();
		String[] groups = result.split("\n\n");
		assertEquals(result, 2, groups.length);
		assertTrue(result, groups[0].startsWith("#actors: 100"));
		assertTrue(result, groups[0].contains("runs   : 2"));
		assertTrue(result, groups[0].contains("mean   : "+format.format(2000.5)));
		assertTrue(result, groups[1].contains("mean   : "+format.format(20)));
	}
}
