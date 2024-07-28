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
package actor4j.benchmark.samples.skynet.with.stopping;

import java.util.concurrent.atomic.AtomicInteger;

import io.actor4j.core.actors.Actor;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorGroup;
import io.actor4j.core.utils.ActorGroupSet;
import io.actor4j.core.utils.HubPattern;

// @see https://github.com/atemerev/skynet
// @see https://github.com/atemerev/skynet/blob/master/java-quasar/src/main/java/Skynet.java
public class Skynet extends Actor {
	public static final int CREATE = 1_000;
	
	public static AtomicInteger count = new AtomicInteger(0);
	
	protected long num;
	protected int size;
	protected int div;
	
	protected long sum;
	
	protected ActorGroup children;
	
	public Skynet(long num, int size, int div) {
		super(String.valueOf(num));
		this.num = num;
		this.size = size;
		this.div = div;
		
		sum = 0L;
		
		children = new ActorGroupSet();
		
		count.incrementAndGet();
	}
	
	@Override
	public void receive(ActorMessage<?> message) {
		if (message.tag() == CREATE) {
			if (size == 1)
				tell(num, 0, getParent());
			else {	
				for (int i = 0; i < div; i++) {
					long subNum = num + i * (size / div);
					children.add(addChild(() -> new Skynet(subNum, size / div, div)));
				}
				new HubPattern(this, children).broadcast(null, CREATE);
			}
		}
		else if (children.remove(message.source())) {
			sum += message.valueAsLong();
			
			if (children.isEmpty()) {
				if (isRootInUser()) {
					System.out.println("result: "+sum);
					BenchmarkSkynetWithStopping.latch.countDown();
				}
				else {
					tell(sum, 0, getParent());
					tell(null, POISONPILL, self()); // with immediate stopping the actor
				}
			}
		}
	}
}