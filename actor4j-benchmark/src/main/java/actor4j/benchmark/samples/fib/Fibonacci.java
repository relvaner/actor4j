/*
 * Copyright (c) 2015-2024, David A. Bauer. All rights reserved.
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
package actor4j.benchmark.samples.fib;

import java.util.concurrent.atomic.AtomicInteger;

import io.actor4j.core.actors.Actor;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorGroup;
import io.actor4j.core.utils.ActorGroupSet;
import io.actor4j.core.utils.HubPattern;

public class Fibonacci extends Actor {
	public static final int CREATE = 10_000;
	
	public static AtomicInteger count = new AtomicInteger(0);
	
	protected long number;
	protected long result;
	
	protected ActorGroup children;
	
	public Fibonacci(long number) {
		super(String.valueOf(number));
		this.number = number;
		
		result = 0L;
		
		children = new ActorGroupSet();
		
		count.incrementAndGet();
	}

	@Override
	public void receive(ActorMessage<?> message) {
		if (message.tag() == CREATE) {
			if (number==0) {
				result = 0;
				tell(result, 0, getParent());
			}
			else if (number==1) {
				result = 1;
				tell(result, 0, getParent());
			}
			else {	
				children.add(addChild(() -> new Fibonacci(number-1)));
				children.add(addChild(() -> new Fibonacci(number-2)));
				new HubPattern(this, children).broadcast(null, CREATE);
			}
		}
		else if (children.remove(message.source())) {
			result += message.valueAsLong();
			
			if (children.isEmpty()) {
				if (isRootInUser()) {
					System.out.println("result: "+result);
					BenchmarkFib.latch.countDown();
				}
				else {
					tell(result, 0, getParent());
					//tell(null, POISONPILL, self()); //with(out) stopping the actor
				}
			}
		}
	}
}