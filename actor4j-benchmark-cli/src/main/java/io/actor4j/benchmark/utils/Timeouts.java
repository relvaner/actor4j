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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

// Runs a part of a benchmark with a time limit (-to, like JMH): a hanging scenario ends the run with an error
// instead of waiting forever. The part runs in its own daemon thread, after the limit it is interrupted.
// The launcher prints only the message of a TimeoutException (with the stack of the hanging thread).
public final class Timeouts {
	// at most this many frames of the hanging thread in the message
	protected static final int MAX_FRAMES = 40;

	// a part did not end within its time limit, the message contains the stack of the hanging thread
	public static class TimeoutException extends IllegalStateException {
		private static final long serialVersionUID = 1L;

		public TimeoutException(String message) {
			super(message);
		}
	}

	private Timeouts() {
	}

	// exceptions of the task are thrown again, a timeout gives IllegalStateException with the description
	public static void run(Runnable task, long timeoutMillis, String description) {
		FutureTask<Void> future = new FutureTask<>(task, null);
		Thread thread = new Thread(future, "actor4j-benchmark-timeout");
		thread.setDaemon(true);
		thread.start();
		try {
			future.get(timeoutMillis, TimeUnit.MILLISECONDS);
		} catch (java.util.concurrent.TimeoutException e) {
			// the stack shows where the scenario hangs (e.g. a latch that is never opened), read before the interrupt
			StackTraceElement[] stack = thread.getStackTrace();
			future.cancel(true);
			StringBuilder message = new StringBuilder(String.format("%s did not end within %s (-to)", description, format(timeoutMillis)));
			message.append(System.lineSeparator()).append("Stack of the hanging thread at the timeout:");
			for (int i=0; i<stack.length && i<MAX_FRAMES; i++)
				message.append(System.lineSeparator()).append("\tat ").append(stack[i]);
			if (stack.length>MAX_FRAMES)
				message.append(System.lineSeparator()).append("\t... ").append(stack.length-MAX_FRAMES).append(" more");
			throw new TimeoutException(message.toString());
		} catch (ExecutionException e) {
			if (e.getCause() instanceof RuntimeException r)
				throw r;
			if (e.getCause() instanceof Error error)
				throw error;
			throw new IllegalStateException(description+" failed", e.getCause());
		} catch (InterruptedException e) {
			future.cancel(true);
			Thread.currentThread().interrupt();
			throw new IllegalStateException(description+" interrupted", e);
		}
	}

	public static String format(long millis) {
		if (millis%60_000==0)
			return (millis/60_000)+" min";
		return millis%1000==0 ? (millis/1000)+" s" : millis+" ms";
	}
}
