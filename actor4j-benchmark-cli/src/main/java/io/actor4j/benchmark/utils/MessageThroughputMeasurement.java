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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import io.actor4j.benchmark.report.BenchmarkOutput;

// Samples a message counter at a fixed rate (deadlines, not delays) and normalizes each sample to the real
// elapsed time. With iterations>0 it ends by itself after that many measured samples (see awaitDone), with
// iterations=0 it runs until stop(). A failure of the counter ends the measurement (see getFailure).
public class MessageThroughputMeasurement {
	// how long stop() waits for the end of the measurement thread
	protected static final long STOP_TIMEOUT_MILLIS = 5_000;

	protected final Supplier<Long> counter;
	protected final int warmupIterations;
	protected final long warmupIntervalNanos;
	protected final int iterations;
	protected final long measurementIntervalNanos;
	protected final DescriptiveStatistics statistics;
	protected final boolean console;

	protected final CountDownLatch done = new CountDownLatch(1);
	protected volatile boolean stopped;
	protected volatile Throwable failure;
	protected Thread thread;

	protected volatile int samples;
	// counter and time at the end of the warmup (or at start without warmup) and at the last measured sample
	protected volatile long baselineCount;
	protected volatile long baselineNanos;
	protected volatile long lastMeasuredCount;
	protected volatile long lastMeasuredNanos;

	public MessageThroughputMeasurement(final Supplier<Long> counter, final int warmupIterations, final long warmupIntervalMillis,
			final int iterations, final long measurementIntervalMillis, final DescriptiveStatistics statistics, final boolean console) {
		this.counter = counter;
		this.warmupIterations = warmupIterations;
		this.warmupIntervalNanos = warmupIntervalMillis*1_000_000;
		this.iterations = iterations;
		this.measurementIntervalNanos = measurementIntervalMillis*1_000_000;
		this.statistics = statistics;
		this.console = console;
	}

	public void start() {
		baselineCount = lastMeasuredCount = counter.get();
		baselineNanos = lastMeasuredNanos = System.nanoTime();

		thread = new Thread(() -> {
			try {
				measure();
			} catch (Throwable t) {
				failure = t;
			} finally {
				done.countDown();
			}
		}, "actor4j-benchmark-throughput");
		thread.setDaemon(true);
		thread.start();
	}

	protected void measure() {
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
			}
			else {
				if (statistics!=null)
					statistics.addValue(perSecond);
				lastMeasuredCount = count;
				lastMeasuredNanos = time;
				samples++;
			}
			if (console)
				System.out.print(BenchmarkOutput.iteration(warmup, warmup ? iteration : iteration-warmupIterations, perSecond, "msg/s"));

			lastCount = count;
			lastTime = time;
			if (!warmup && iterations>0 && samples>=iterations)
				break;
		}
	}

	// ends the measurement and waits for the end of the measurement thread, so that the statistics are complete
	public void stop() {
		stopped = true;
		if (thread==null || thread==Thread.currentThread())
			return;
		LockSupport.unpark(thread);
		boolean interrupted = Thread.interrupted();
		try {
			thread.join(STOP_TIMEOUT_MILLIS);
		} catch (InterruptedException e) {
			interrupted = true;
		}
		if (interrupted)
			Thread.currentThread().interrupt();
	}

	// waits until all measured samples are taken (iterations>0), stop() was called or the counter failed
	public void awaitDone() throws InterruptedException {
		done.await();
	}

	// like awaitDone, at most timeoutMillis, false after the timeout
	public boolean awaitDone(long timeoutMillis) throws InterruptedException {
		return done.await(timeoutMillis, TimeUnit.MILLISECONDS);
	}

	// the exception of the counter, null without failure
	public Throwable getFailure() {
		return failure;
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
