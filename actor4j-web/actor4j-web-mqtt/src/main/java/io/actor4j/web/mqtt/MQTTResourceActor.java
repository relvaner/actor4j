/*
 * Copyright (c) 2015-2023, David A. Bauer. All rights reserved.
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
package io.actor4j.web.mqtt;

import static io.actor4j.core.logging.ActorLogger.logger;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import io.actor4j.core.actors.ResourceActor;
import io.actor4j.core.messages.ActorMessage;
import static io.actor4j.core.messages.ActorReservedTag.*;

import java.util.Set;

public abstract class MQTTResourceActor extends ResourceActor {
	public static final int PUBLISH     = RESERVED_PUBSUB_PUBLISH;
	public static final int SUBSCRIBE   = RESERVED_PUBSUB_SUBSCRIBE;
	public static final int UNSUBSCRIBE = RESERVED_PUBSUB_UNSUBSCRIBE;
	
	protected volatile MqttClient client;
	protected final String broker;

	public MQTTResourceActor(String broker) {
		this(null, broker);
	}

	public MQTTResourceActor(String name, String broker) {
		super(name, true, false);
		this.broker = broker;
	}
	
	public abstract void configure(MqttConnectOptions connectOptions);
	public abstract MqttCallback callback();

	@Override
	public void preStart() {
		super.preStart();
		
		try {
			client = new MqttClient(broker, clientId(), new MemoryPersistence());
			MqttConnectOptions connectOptions = new MqttConnectOptions();
			configure(connectOptions);
			client.setCallback(callback());
			client.connect(connectOptions);
			logger().info(String.format("%s - MQTT-Service started...", name));
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void postStop() {
		if (client!=null)
			try {
				client.disconnect();
				client.close();
				logger().info(String.format("%s - MQTT-Service stopped...", name));
			} catch (MqttException e) {
				e.printStackTrace();
			}
	}
	
	@Override
	public void receive(ActorMessage<?> message) {
		reconnect();
		
		if (message.tag()==PUBLISH && message.value()!=null && message.value() instanceof MQTTPublish)
			publish((MQTTPublish)message.value());
		else if (message.tag()==SUBSCRIBE && message.value()!=null && message.value() instanceof String)
			subscribe(message.valueAsString());
		else if (message.tag()==UNSUBSCRIBE && message.value()!=null && message.value() instanceof String)
			unsubscribe(message.valueAsString());
		else
			unhandled(message);
	}
	
	protected void reconnect() {
		if (!client.isConnected())
			try {
				client.reconnect();
			} catch (MqttException e) {
				e.printStackTrace();
			}
	}
	
	public void publish(MQTTPublish publish) {
		try {
			client.publish(publish.topic(), publish.payload(), publish.qos(), publish.retained());
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
	
	public void subscribe(String topicFilter) {
		try {
			client.subscribe(topicFilter);
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
	
	public void subscribe(Set<String> topicFilters) {
		try {
			client.subscribe((String[]) topicFilters.toArray());
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
	
	public void unsubscribe(String topicFilter) {
		try {
			client.unsubscribe(topicFilter);
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
	
	public void unsubscribe(Set<String> topicFilters) {
		try {
			client.unsubscribe((String[]) topicFilters.toArray());
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
	
	public abstract String clientId();
}
