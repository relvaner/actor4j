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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class MessageThroughputMeasurement {
	protected final Timer timer;
	protected final TimerTask timerTask;
	protected final long intervalMillis;
	protected volatile int samples;

	public MessageThroughputMeasurement(final Supplier<Long> counter, final int warmupIterations, final AtomicLong warmupCounter, final DescriptiveStatistics statistics, final boolean console) {
		this(counter, warmupIterations, warmupCounter, statistics, console, 1000);
	}
	
	public MessageThroughputMeasurement(final Supplier<Long> counter, final int warmupIterations, final AtomicLong warmupCounter, final DescriptiveStatistics statistics, final boolean console, final long intervalMillis) {
		final DecimalFormat decimalFormat = new DecimalFormat("###,###,###");
		this.intervalMillis = intervalMillis;
		
		timer = new Timer("actor4j-benchmark-throughput", true);
		timerTask = new TimerTask() {
			protected int iteration = 1; 
			protected long lastCount = counter.get();
			protected long lastTime = System.nanoTime();
			@Override
			public void run() {
				long count = counter.get();
				long time  = System.nanoTime();
				// normalized to the real elapsed time (GC, load may delay a tick)
				long perSecond = Math.round((count-lastCount)*1e9/(time-lastTime));
				
				boolean warmup = iteration<=warmupIterations;
				if (warmup) {
					if (warmupCounter!=null)
						warmupCounter.set(count);
				}
				else {
					if (statistics!=null)
						statistics.addValue(perSecond);
					samples++;
				}
				if (console)
					System.out.printf(warmup ? "Warmup %-2d : %s msg/s%n" : "%-2d : %s msg/s%n", 
						warmup ? iteration : iteration-warmupIterations, decimalFormat.format(perSecond));
				
				lastCount = count;
				lastTime = time;
				iteration++;
			}
		};
	}

	public void start() {
		timer.scheduleAtFixedRate(timerTask, intervalMillis, intervalMillis);
	}
	
	public void stop() {
		timer.cancel();
	}
	
	// number of measured (non-warmup) ticks, varies with the stop time
	public int getSamples() {
		return samples;
	}
}
