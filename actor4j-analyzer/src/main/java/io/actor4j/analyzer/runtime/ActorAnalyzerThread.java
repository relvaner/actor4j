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
package io.actor4j.analyzer.runtime;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import io.actor4j.core.runtime.ActorThreadMode;
import io.actor4j.core.runtime.InternalActorCell;
import io.actor4j.core.runtime.InternalActorSystem;
import io.actor4j.core.messages.ActorMessage;

public abstract class ActorAnalyzerThread extends Thread {
	protected final Queue<ActorMessage<?>> outerQueueL2;
	protected final Queue<ActorMessage<?>> outerQueueL1;
	
	protected InternalActorSystem system;
	
	protected final AtomicLong counter;
	
	protected final Timer timer;
	protected final TimerTask timerTask;
	protected final long period;
	
	public ActorAnalyzerThread(long period) {
		super("actor4j-analyzer-thread");
		
		outerQueueL2 = new ConcurrentLinkedQueue<>();
		outerQueueL1 = new LinkedList<>();
		
		counter = new AtomicLong(0);
		
		timer = new Timer();
		timerTask = new TimerTask() {
			@Override
			public void run() {
				((AnalyzerActorSystemImpl)system).getActorCells();
				
				update(((AnalyzerActorSystemImpl)system).getActorCells());
			}
		};
		this.period = period;
	}
	
	protected void setSystem(InternalActorSystem system) {
		this.system = system;
	}
	
	protected boolean poll(Queue<ActorMessage<?>> queue) {
		boolean result = false;
		
		ActorMessage<?> message = queue.poll();
		if (message!=null) {
			analyze(message);
			counter.getAndIncrement();
			
			result = true;
		} 
		
		return result;
	}
	
	protected abstract void analyze(ActorMessage<?> message);
	
	protected abstract void update(Set<InternalActorCell> actorCells);
	
	@Override
	public void run() {
		timer.schedule(timerTask, 0, period);
		
		boolean hasNextOuter  = false;
		
		while (!isInterrupted()) { 
			hasNextOuter = poll(outerQueueL1);
			if (!hasNextOuter && outerQueueL2.peek()!=null) {
				ActorMessage<?> message = null;
				for (int j=0; (message=outerQueueL2.poll())!=null && j<10000; j++)
					outerQueueL1.offer(message);
				
				hasNextOuter = poll(outerQueueL1);
			}
			
			if ((!hasNextOuter))
				if (system.getConfig().threadMode()==ActorThreadMode.YIELD)
					Thread.yield();
				else {
					try {
						sleep(system.getConfig().sleepTime());
					} catch (InterruptedException e) {
						interrupt();
					}
				}
		}
		
		timer.cancel();
	}
	
	public Queue<ActorMessage<?>> getOuterQueue() {
		return outerQueueL2;
	}
}
