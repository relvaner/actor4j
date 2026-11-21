package io.actor4j.benchmark.cli;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import io.actor4j.benchmark.BenchmarkConfig;
import io.actor4j.benchmark.ThroughputBenchmark;
import io.actor4j.benchmark.utils.MessageThroughputMeasurement;

public class ThroughputBenchmarkTest {
	// counter that grows with the elapsed time: about 1,000,000 per second
	static long timeCounter(long start) {
		return (System.nanoTime()-start)/1000;
	}
	
	@Test
	public void test_exact_number_of_samples_with_own_warmup_interval() {
		long start = System.nanoTime();
		BenchmarkConfig config = new BenchmarkConfig().warmupIterations(2).warmupTime(30).iterations(3).measurementTime(60);
		AtomicBoolean stopped = new AtomicBoolean();
		AtomicBoolean shutdown = new AtomicBoolean();
		
		long begin = System.nanoTime();
		ThroughputBenchmark.Result result = new ThroughputBenchmark(config, "test", () -> timeCounter(start))
			.console(false)
			.onStop(() -> { assertFalse(shutdown.get()); stopped.set(true); })
			.onShutdown(() -> shutdown.set(true))
			.start();
		long millis = (System.nanoTime()-begin)/1_000_000;
		
		assertEquals(3, result.samples());
		assertEquals(3, result.statistics().getN());
		assertTrue(stopped.get());
		assertTrue(shutdown.get());
		// 2*30 + 3*60 = 240 ms
		assertTrue("took "+millis, millis>=230 && millis<1000);
		// count over the measurement only (3*60 ms at 1e6/s = 180,000), rate per second, not per sample
		assertEquals(180_000, result.count(), 30_000);
		assertEquals(1_000_000, result.perSecond(), 100_000);
		assertEquals(1_000_000, result.statistics().getMean(), 100_000);
	}
	
	@Test
	public void test_shutdown_after_failure() {
		AtomicBoolean shutdown = new AtomicBoolean();
		BenchmarkConfig config = new BenchmarkConfig().warmupIterations(0).iterations(1).measurementTime(10);
		try {
			new ThroughputBenchmark(config, "test", () -> 0L).console(false)
				.onStop(() -> { throw new IllegalStateException("failed"); })
				.onShutdown(() -> shutdown.set(true))
				.start();
			fail();
		} catch (IllegalStateException e) {
			assertTrue(shutdown.get());
		}
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void test_iterations_required() {
		new ThroughputBenchmark(new BenchmarkConfig().iterations(0), "test", () -> 0L).console(false).start();
	}
	
	@Test
	public void test_open_ended_measurement_until_stop() throws InterruptedException {
		AtomicLong counter = new AtomicLong();
		MessageThroughputMeasurement measurement = new MessageThroughputMeasurement(() -> counter.addAndGet(100), 0, null, null, false, 20);
		measurement.start();
		Thread.sleep(150);
		measurement.stop();
		measurement.awaitDone();
		assertTrue(measurement.getSamples()>=4);
	}
	
	@Test
	public void test_param_as_int() {
		BenchmarkConfig config = new BenchmarkConfig().param1(" 30 ").param2("x");
		assertEquals(30, config.param1AsInt("number"));
		try {
			config.param2AsInt("hops");
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Missing or invalid -param2 (hops): 'x'", e.getMessage());
		}
	}
}
