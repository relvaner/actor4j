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

public record KafkaProducerRecord<K, V>(K key, V value, String topic) {
	public KafkaProducerRecord(K key, V value) {
		this(key, value, null);
	}
	
	public static <K, V> KafkaProducerRecord<K, V> of(K key, V value, String topic) {
		return new KafkaProducerRecord<K, V>(key, value, topic);
	}
	
	public static <K, V> KafkaProducerRecord<K, V> of(K key, V value) {
		return new KafkaProducerRecord<K, V>(key, value);
	}
}
