/*
 * Copyright (c) 2015-2017, David A. Bauer. All rights reserved.
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
	protected Timer timer;
	protected TimerTask timerTask;

	public MessageThroughputMeasurement(final Supplier<Long> counter, final long warmupIterations, final AtomicLong warmupCount, final DescriptiveStatistics statistics, final boolean console) {
		final DecimalFormat decimalFormat = new DecimalFormat("###,###,###");
		
		timer = new Timer();
		timerTask = new TimerTask() {
			protected int iteration = 1; 
			protected long lastCount;
			@Override
			public void run() {
				long count = counter.get();
				long diff  = count-lastCount;
				
				if (statistics!=null && iteration>warmupIterations)
					statistics.addValue(diff);
				else
					warmupCount.set(count);
				if (console && iteration>warmupIterations)
					System.out.printf("%-2d : %s msg/s%n", iteration-warmupIterations, decimalFormat.format(diff));
				else
					System.out.printf("Warmup %-2d : %s msg/s%n", iteration, decimalFormat.format(diff));
				
				lastCount = count;
				iteration++;
			}
		};
	}

	public void start() {
		timer.schedule(timerTask, 0, 1000);
	}
	
	public void stop() {
		timer.cancel();
	}
}
