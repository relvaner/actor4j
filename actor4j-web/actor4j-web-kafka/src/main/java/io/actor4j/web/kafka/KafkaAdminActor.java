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

import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.common.KafkaException;

import io.actor4j.core.actors.ResourceActor;
import io.actor4j.core.messages.ActorMessage;

import static io.actor4j.core.messages.ActorReservedTag.*;

//import static io.actor4j.core.logging.ActorLogger.*;

// https://docs.confluent.io/kafka/kafka-apis.html#admin-api
public class KafkaAdminActor extends ResourceActor {
	public static final int TOPIC  = RESERVED_PUBSUB_TOPIC;
	public static final int TOPICS = RESERVED_PUBSUB_TOPICS;
	
	protected final String broker;
	protected final Properties config;
	
	protected /*quasi final*/AdminClient adminClient;
	
	public KafkaAdminActor(String broker) {
		this(null, broker);
	}

	public KafkaAdminActor(String name, String broker) {
		super(name, true, false);
		
		this.broker = broker;
		
		config = new Properties(); 
	}
	
	public void configure(Properties config) {
		// empty
	}
	
	@Override
	public void preStart() {
		super.preStart();

		config.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, broker);
		configure(config);
		
		adminClient = AdminClient.create(config);
	}
	
	@Override
	public void receive(ActorMessage<?> message) {
		if (message.tag()==TOPIC && message.value()!=null && message.value() instanceof KafkaAdminTopic newTopic)
			adminClient.createTopics(List.of(newTopic.topic()));
		else if (message.tag()==TOPICS && message.value()!=null && message.value() instanceof KafkaAdminTopics newTopics)
			adminClient.createTopics(newTopics.topics());
		else
			unhandled(message);
	}
	
	@Override
	public void postStop() {
		if (adminClient!=null)
			try {
				adminClient.close();
			}
			catch(KafkaException e) {
				e.printStackTrace();
			}
	}
}
