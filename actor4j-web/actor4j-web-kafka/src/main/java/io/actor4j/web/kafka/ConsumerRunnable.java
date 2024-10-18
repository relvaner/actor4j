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
package io.actor4j.web.kafka;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;

import static io.actor4j.core.logging.ActorLogger.*;

public abstract class ConsumerRunnable<K, V> implements Runnable {
	private final KafkaConsumer<K, V> consumer;
	private final CountDownLatch shutdownLatch;

	public ConsumerRunnable(KafkaConsumer<K, V> consumer) {
		this.consumer = consumer;
		this.shutdownLatch = new CountDownLatch(1);
	}

	public abstract void process(ConsumerRecord<K, V> record);

	public void run() {
		try {
			while (!Thread.currentThread().isInterrupted()) {
				ConsumerRecords<K, V> records = consumer.poll(Duration.ofMillis(Long.MAX_VALUE));
				records.forEach(record -> process(record));
				consumer.commitAsync();
			}
		} catch (WakeupException e) {
			// ignore, we're closing
		} catch (Exception e) {
			logger().log(DEBUG, String.format("Unexpected error: %s", e));
		} finally {
			consumer.close();
			shutdownLatch.countDown();
		}
	}

	public void cancel()  {
		consumer.wakeup();
		try {
			shutdownLatch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}