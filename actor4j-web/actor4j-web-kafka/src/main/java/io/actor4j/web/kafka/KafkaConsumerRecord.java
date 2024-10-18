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

import org.apache.kafka.clients.consumer.ConsumerRecord;

public record KafkaConsumerRecord<K, V>(K key, V value, long offset, int partition, String topic) {
	public static <K, V> KafkaConsumerRecord<K, V> of(K key, V value, long offset, int partition, String topic) {
		return new KafkaConsumerRecord<>(key, value, offset, partition, topic);
	}
	
	public static <K, V> KafkaConsumerRecord<K, V> of(ConsumerRecord<K, V> record) {
		return new KafkaConsumerRecord<>(record.key(), record.value(), record.offset(), record.partition(), record.topic());
	}
}
