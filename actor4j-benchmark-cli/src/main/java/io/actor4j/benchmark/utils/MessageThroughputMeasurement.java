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
package io.actor4j.benchmark.utils;

import java.text.DecimalFormat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

// Samples a message counter at a fixed rate (deadlines, not delays) and normalizes each sample to the real
// elapsed time. With iterations>0 it ends by itself after that many measured samples (see awaitDone).
public class MessageThroughputMeasurement {
	protected final Supplier<Long> counter;
	protected final int warmupIterations;
	protected final long warmupIntervalNanos;
	protected final int iterations;
	protected final long measurementIntervalNanos;
	protected final AtomicLong warmupCounter;
	protected final DescriptiveStatistics statistics;
	protected final boolean console;

	protected final CountDownLatch done = new CountDownLatch(1);
	protected volatile boolean stopped;
	protected Thread thread;

	protected volatile int samples;
	// counter and time at the end of the warmup (or at start without warmup) and at the last measured sample
	protected volatile long baselineCount;
	protected volatile long baselineNanos;
	protected volatile long lastMeasuredCount;
	protected volatile long lastMeasuredNanos;

	public MessageThroughputMeasurement(final Supplier<Long> counter, final int warmupIterations, final AtomicLong warmupCounter, final DescriptiveStatistics statistics, final boolean console) {
		this(counter, warmupIterations, warmupCounter, statistics, console, 1000);
	}

	public MessageThroughputMeasurement(final Supplier<Long> counter, final int warmupIterations, final AtomicLong warmupCounter, final DescriptiveStatistics statistics, final boolean console, final long intervalMillis) {
		this(counter, warmupIterations, intervalMillis, 0, intervalMillis, warmupCounter, statistics, console);
	}

	public MessageThroughputMeasurement(final Supplier<Long> counter, final int warmupIterations, final long warmupIntervalMillis, final int iterations, final long measurementIntervalMillis,
			final AtomicLong warmupCounter, final DescriptiveStatistics statistics, final boolean console) {
		this.counter = counter;
		this.warmupIterations = warmupIterations;
		this.warmupIntervalNanos = warmupIntervalMillis*1_000_000;
		this.iterations = iterations;
		this.measurementIntervalNanos = measurementIntervalMillis*1_000_000;
		this.warmupCounter = warmupCounter;
		this.statistics = statistics;
		this.console = console;
	}

	public void start() {
		final DecimalFormat decimalFormat = new DecimalFormat("###,###,###");

		baselineCount = lastMeasuredCount = counter.get();
		baselineNanos = lastMeasuredNanos = System.nanoTime();
		if (warmupCounter!=null)
			warmupCounter.set(baselineCount);

		thread = new Thread(() -> {
			long lastCount = baselineCount;
			long lastTime  = baselineNanos;
			long deadline  = baselineNanos;
			for (int iteration=1; !stopped; iteration++) {
				boolean warmup = iteration<=warmupIterations;
				deadline += warmup ? warmupIntervalNanos : measurementIntervalNanos;
				long wait;
				while (!stopped && (wait = deadline-System.nanoTime())>0)
					LockSupport.parkNanos(wait);
				if (stopped)
					break;

				long count = counter.get();
				long time  = System.nanoTime();
				long perSecond = Math.round((count-lastCount)*1e9/(time-lastTime));

				if (warmup) {
					baselineCount = count;
					baselineNanos = time;
					if (warmupCounter!=null)
						warmupCounter.set(count);
				}
				else {
					if (statistics!=null)
						statistics.addValue(perSecond);
					lastMeasuredCount = count;
					lastMeasuredNanos = time;
					samples++;
				}
				if (console)
					System.out.printf(warmup ? "Warmup %-2d : %s msg/s%n" : "%-2d : %s msg/s%n",
						warmup ? iteration : iteration-warmupIterations, decimalFormat.format(perSecond));

				lastCount = count;
				lastTime = time;
				if (!warmup && iterations>0 && samples>=iterations)
					break;
			}
			done.countDown();
		}, "actor4j-benchmark-throughput");
		thread.setDaemon(true);
		thread.start();
	}

	public void stop() {
		stopped = true;
		if (thread!=null)
			LockSupport.unpark(thread);
	}

	// waits until all measured samples are taken (iterations>0) or stop() was called
	public void awaitDone() throws InterruptedException {
		done.await();
	}

	// number of measured (non-warmup) samples
	public int getSamples() {
		return samples;
	}

	// messages between the end of the warmup and the last measured sample
	public long getMeasuredCount() {
		return lastMeasuredCount-baselineCount;
	}

	// throughput over the whole measurement (not the mean of the samples)
	public double getMeasuredPerSecond() {
		long nanos = lastMeasuredNanos-baselineNanos;
		return nanos>0 ? getMeasuredCount()*1e9/nanos : 0;
	}
}
