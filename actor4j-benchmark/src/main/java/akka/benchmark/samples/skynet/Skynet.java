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
package akka.benchmark.samples.skynet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.benchmark.ActorMessage;
import akka.routing.ActorRefRoutee;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;

// @see https://github.com/atemerev/skynet
// @see https://github.com/atemerev/skynet/blob/master/java-quasar/src/main/java/Skynet.java
public class Skynet extends UntypedAbstractActor {
	public static final int CREATE = 1_000;
	
	public static AtomicInteger count = new AtomicInteger(0);
	
	protected long num;
	protected int size;
	protected int div;
	
	protected long sum;
	
	protected Set<ActorRef> children;
	protected List<Routee> routees;
	
	public Skynet(long num, int size, int div) {
		super();
		this.num = num;
		this.size = size;
		this.div = div;
		
		sum = 0L;
		
		children = new HashSet<>();
		routees = new ArrayList<Routee>();
		
		count.incrementAndGet();
	}
	
	@Override
	public void onReceive(Object message) {
		if (message instanceof ActorMessage) {
			if (((ActorMessage) message).tag == CREATE) {
				if (size == 1)
					context().parent().tell(new ActorMessage(num, 0), getSelf());
				else {	
					for (int i = 0; i < div; i++) {
						long subNum = num + i * (size / div);
						ActorRef child = 
								context().actorOf(Props.create(Skynet.class, subNum, size / div, div).withDispatcher("my-dispatcher"));
						children.add(child);
						routees.add(new ActorRefRoutee(child));
					}
					new Router(new BroadcastRoutingLogic(), routees).route(new ActorMessage(null, CREATE), getSelf());
				}
			}
			else if (children.remove(getSender())) {
				sum += ((ActorMessage) message).valueAsLong();
				
				if (children.isEmpty()) {
					if (context().parent().path().toString().equals("akka://"+context().system().name()+"/user")/*is user guardian*/) {
						System.out.println("result: "+sum);
						BenchmarkSkynet.latch.countDown();
					}
					else {
						context().parent().tell(new ActorMessage(sum, 0), getSelf());
						//context().stop(getSelf()); //with(out) stopping the actor
					}
				}
			}
		}
	}
}