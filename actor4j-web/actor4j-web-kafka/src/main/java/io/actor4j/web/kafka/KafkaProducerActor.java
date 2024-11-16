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

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;

import io.actor4j.core.actors.ResourceActor;
import io.actor4j.core.messages.ActorMessage;

import static io.actor4j.core.messages.ActorReservedTag.*;

//import static io.actor4j.core.logging.ActorLogger.*;

public abstract class KafkaProducerActor<K, V> extends ResourceActor {
	public static final int PUBLISH = RESERVED_PUBSUB_PUBLISH;
	
	protected final String broker;
	protected final Properties config;
	protected final String clientId;
	
	protected /*quasi final*/KafkaProducer<K, V> producer;
	
	public KafkaProducerActor(String broker) {
		this(null, broker, null);
	}
	
	public KafkaProducerActor(String name, String broker) {
		this(name, broker, name);
	}

	public KafkaProducerActor(String name, String broker, String clientId) {
		super(name, true, false);
		
		this.broker = broker;
		this.clientId = clientId;
		
		config = new Properties(); 
	}
	
	public abstract void configure(Properties config);
	public abstract Callback callback();
	
	@Override
	public void preStart() {
		super.preStart();
		
		config.put(CommonClientConfigs.CLIENT_ID_CONFIG, clientId());
		config.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, broker);
		configure(config);
		
		producer = new KafkaProducer<>(config);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void receive(ActorMessage<?> message) {
		if (message.tag()==PUBLISH && message.value()!=null && message.value() instanceof KafkaProducerRecord r) {
			ProducerRecord<K, V> record = handleMessage(r);
			if (record!=null)
				send(record);
		}
		else
			unhandled(message);
	}
	
	public abstract ProducerRecord<K, V> handleMessage(KafkaProducerRecord<K, V> record);
	
	public void send(ProducerRecord<K, V> record) {
		producer.send(record, callback());
	}
	
	@Override
	public void postStop() {
		if (producer!=null)
			try {
				producer.close();
			}
			catch(KafkaException e) {
				e.printStackTrace();
			}
	}
	
	public String clientId() {
		return clientId;
	}
}
