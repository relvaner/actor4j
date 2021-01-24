/*
 * Copyright (c) 2016-2019, David A. Bauer. All rights reserved.
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
package io.actor4j.examples.apc.actor;

import static io.actor4j.core.utils.ActorLogger.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import io.actor4j.apc.actor.APCActorRef;
import io.actor4j.apc.actor.APCActorSystem;
import io.actor4j.apc.actor.APCObject;

public class ExampleAPCActor {
	public static interface Greeter {
		void sayGreeting();
		void sayGreeting(String name);
		void sayGreeting(Integer number);
		void sayGreeting(Integer number, String alias);
		Future<Integer> task(Integer number);
	}
	
	public static class GreeterImpl extends APCObject implements Greeter {
		@Override
		public void sayGreeting() {
			logger().debug(String.format("sayGreeting: Hello Developer!"));
		}
		
		@Override
		public void sayGreeting(String name) {
			logger().debug(String.format("sayGreeting: Hello %s", name));
		}

		@Override
		public void sayGreeting(Integer number) {
			logger().debug(String.format("sayGreeting: Hello Number %d", number));
		}
		
		@Override
		public void sayGreeting(Integer number, String alias) {
			logger().debug(String.format("sayGreeting: Hello Number %d (%s)", number, alias));
		}
		
		@Override
		public Future<Integer> task(Integer number) {
			return handleFuture((f) -> f.complete(number+1));
		}
	}
	
	public ExampleAPCActor() {
		APCActorSystem system = new APCActorSystem("actor4j-apc-actor");
		APCActorRef<Greeter> ref = system.addAPCActor(Greeter.class, new GreeterImpl());
		
		ref.tell().sayGreeting();
		ref.tell().sayGreeting("David");
		ref.tell().sayGreeting(5);
		ref.tell().sayGreeting(5, "Robot");
		
		system.start();
		
		try {
			logger().debug(String.format("task: Result is %d", ref.tell().task(41).get()));
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		system.shutdownWithActors(true);
	}

	public static void main(String[] args) {
		new ExampleAPCActor();
	}
}
