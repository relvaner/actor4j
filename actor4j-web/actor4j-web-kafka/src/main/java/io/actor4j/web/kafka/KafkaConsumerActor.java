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

import java.util.Properties;
import java.util.Set;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Callback;

import io.actor4j.core.actors.ResourceActor;
import io.actor4j.core.messages.ActorMessage;

import static io.actor4j.core.messages.ActorReservedTag.*;

public abstract class KafkaConsumerActor<K, V> extends ResourceActor {
	public static final int PUBLISH = RESERVED_PUBSUB_PUBLISH;
	public static final int TOPIC   = RESERVED_PUBSUB_TOPIC;
	
	protected final String broker;
	protected final Properties config;

	protected final Set<String> topics;
	protected /*quasi final*/ KafkaConsumer<K, V> consumer;
	protected /*quasi final*/ ConsumerRunnable<K, V> consumerRunnable;
	
	public KafkaConsumerActor(String broker, String topic) {
		this(null, broker, topic);
	}

	public KafkaConsumerActor(String name, String broker, String topic) {
		this(name, broker, Set.of(topic));
	}
	
	public KafkaConsumerActor(String broker, Set<String> topics) {
		this(null, broker, topics);
	}
	
	public KafkaConsumerActor(String name, String broker, Set<String> topics) {
		super(name, true, false);
		
		this.broker = broker;
		this.topics = topics;
		
		config = new Properties();
	}
	
	public abstract void configure(Properties config);
	
	public void configure(KafkaConsumer<K, V> consumer) {
		// default handling, overwrite if you want to assign to partitions directly
		consumer.subscribe(topics);
	}
	
	public abstract Callback callback();
	
	@Override
	public void preStart() {
		super.preStart();
		
		config.put(CommonClientConfigs.CLIENT_ID_CONFIG, clientId());
		config.put(CommonClientConfigs.GROUP_ID_CONFIG, groupId());
		config.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, broker);
		configure(config);
		
		consumer = new KafkaConsumer<>(config);
		configure(consumer);
		consumerRunnable = new ConsumerRunnable<>(consumer) {
			@Override
			public void process(ConsumerRecord<K, V> record) {
				getSystem().send(ActorMessage.create(KafkaConsumerRecord.of(record), PUBLISH, self(), self()));
			}
		};
		
		Thread.ofVirtual().start(consumerRunnable);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void receive(ActorMessage<?> message) {
		if (message.tag()==TOPIC && message.value() instanceof String) {
			topics.add(message.valueAsString());
			consumer.subscribe(topics);
		}
		if (message.tag()==PUBLISH && message.isSelfReferencing(self()) && message.value()!=null && message.value() instanceof KafkaConsumerRecord r) {
			handleMessage((KafkaConsumerRecord<K, V>)r);
		}
		else
			unhandled(message);
	}
	
	public abstract void handleMessage(KafkaConsumerRecord<K, V> record);
	
	@Override
	public void postStop() {
		if (consumerRunnable!=null)
			consumerRunnable.cancel();
	}
	
	public abstract String clientId();
	public abstract String groupId();
}
