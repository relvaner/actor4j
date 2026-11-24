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

public class TimeMeasurement {
	protected long timeNanos  = 0;
	protected long startNanos = 0;
	protected boolean started;
	protected boolean measuring;

	public void start() {
		startNanos = System.nanoTime();
		started = true;
		measuring = true;
	}

	public void stop() {
		if (!measuring)
			throw new IllegalStateException("TimeMeasurement: stop() without start()");
		timeNanos = System.nanoTime()-startNanos;
		measuring = false;
	}

	public boolean isMeasuring() {
		return measuring;
	}

	// start() and stop() were called since the last reset
	public boolean isMeasured() {
		return started && !measuring;
	}

	public void reset() {
		timeNanos  = 0;
		startNanos = 0;
		started    = false;
		measuring  = false;
	}

	public long getTimeMillis() {
		return timeNanos/1_000_000;
	}

	public long getTimeNanos() {
		return timeNanos;
	}

	public double getTimeMillisAsDouble() {
		return timeNanos/1e6;
	}
}
