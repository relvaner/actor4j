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
package akka.benchmark.samples.fib.with.stopping;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.benchmark.ActorMessage;
import akka.benchmark.BenchmarkSampleAkka;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import shared.benchmark.Benchmark;
import shared.benchmark.BenchmarkConfig;

public class BenchmarkFibWithStopping extends BenchmarkSampleAkka {
	public static CountDownLatch latch;
	
	public BenchmarkFibWithStopping(BenchmarkConfig config) {
		super(config);
		
		ActorSystem system = ActorSystem.create("akka-benchmark-fibonacci-with-stopping", akkaConfig);
		
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() { 
			@Override
			public void run() {
				System.out.printf("#actors : %s%n", Fibonacci.count);
			}
		}, 1000, 1000);
		
		Benchmark benchmark = new Benchmark(config);
		
		benchmark.start((timeMeasurement, iteration) -> {
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
		});
		
		timer.cancel();
		try {
			Await.result(system.terminate(), Duration.create(30, TimeUnit.SECONDS));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		new BenchmarkFibWithStopping(new BenchmarkConfig(10, 60_000, "30")); // 10 + 60 iterations!
	}
}
