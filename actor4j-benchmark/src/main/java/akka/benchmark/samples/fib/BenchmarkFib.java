/*
 * Copyright (c) 2015-2020, David A. Bauer. All rights reserved.
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

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.benchmark.ActorMessage;
import akka.benchmark.BenchmarkSampleAkka;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import shared.benchmark.Benchmark;
import shared.benchmark.BenchmarkConfig;

public class BenchmarkFib extends BenchmarkSampleAkka {
	public static CountDownLatch latch;
	
	public BenchmarkFib(BenchmarkConfig config) {
		super(config);
		
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() { 
			@Override
			public void run() {
				System.out.printf("#actors : %s%n", Fibonacci.count);
			}
		}, 0, 1000);
		
		Benchmark benchmark = new Benchmark(config);
		
		benchmark.start((timeMeasurement, iteration) -> {
			ActorSystem system = ActorSystem.create("akka-benchmark-fibonacci", akkaConfig);
			latch = new CountDownLatch(1);
			
			timeMeasurement.start();
			ActorRef fibonacci = system.actorOf(Props.create(Fibonacci.class, Long.valueOf(config.param1)).withDispatcher("my-dispatcher"));
			fibonacci.tell(new ActorMessage(null, Fibonacci.CREATE), fibonacci);
			try {
				latch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			timeMeasurement.stop();

			System.out.printf("#actors : %s%n", Fibonacci.count);
			Fibonacci.count.getAndSet(0);
			
			fibonacci.tell(PoisonPill.getInstance(), fibonacci); // stop all actors from parent
			try {
				Await.result(system.terminate(), Duration.create(30, TimeUnit.SECONDS));
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		
		timer.cancel();
	}
	
	public static void main(String[] args) {
		new BenchmarkFib(new BenchmarkConfig(10, 60_000, "30")); // 10 + 60 iterations!
	}
}
