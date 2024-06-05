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
package akka.benchmark.samples.fib;

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

public class Fibonacci extends UntypedAbstractActor {
	public static final int CREATE = 10_000;
	
	public static AtomicInteger count = new AtomicInteger(0);
	
	protected long number;
	protected long result;
	
	protected Set<ActorRef> children;
	protected List<Routee> routees;
	
	public Fibonacci(long number) {
		super();
		this.number = number;
		
		result = 0L;
		
		children = new HashSet<>();
		routees = new ArrayList<Routee>();
		
		count.incrementAndGet();
	}
	
	@Override
	public void onReceive(Object message) {
		if (message instanceof ActorMessage) {
			if (((ActorMessage) message).tag == CREATE) {
				if (number==0) {
					result = 0;
					context().parent().tell(new ActorMessage(result, 0), getSelf());
				}
				else if (number==1) {
					result = 1;
					context().parent().tell(new ActorMessage(result, 0), getSelf());
				}
				else {	
					ActorRef fib1 = context().actorOf(Props.create(Fibonacci.class, number-1).withDispatcher("my-dispatcher"));
					ActorRef fib2 = context().actorOf(Props.create(Fibonacci.class, number-2).withDispatcher("my-dispatcher"));
					children.add(fib1);
					children.add(fib2);
					routees.add(new ActorRefRoutee(fib1));
					routees.add(new ActorRefRoutee(fib2));
					new Router(new BroadcastRoutingLogic(), routees).route(new ActorMessage(null, CREATE), getSelf());
				}
			}
			else if (children.remove(getSender())) {
				result += ((ActorMessage) message).valueAsLong();
				
				if (children.isEmpty()) {
					if (context().parent().path().toString().equals("akka://"+context().system().name()+"/user")/*is user guardian*/) {
						System.out.println("result: "+result);
						BenchmarkFib.latch.countDown();
					}
					else {
						context().parent().tell(new ActorMessage(result, 0), getSelf());
						//context().stop(getSelf()); //with(out) stopping the actor
					}
				}
			}
		}
	}
}