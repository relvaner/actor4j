/*
 * Copyright (c) 2015-2018, David A. Bauer. All rights reserved.
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
package io.actor4j.examples.mqtt;

import static io.actor4j.web.mqtt.MQTTResourceActor.*;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import io.actor4j.core.ActorSystem;
import io.actor4j.core.actors.Actor;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.web.mqtt.MQTTPublish;
import io.actor4j.web.mqtt.MQTTResourceActor;

public class ExampleMQTT {
	public ExampleMQTT() {
		super();
		
		ActorSystem system = new ActorSystem();
		
		CountDownLatch done = new CountDownLatch(2);
		
		UUID receiver = system.addActor(() -> new Actor("receiver") {
			@Override
			public void receive(ActorMessage<?> message) {
				if (message.tag==PUBLISH && message.value!=null) {
					System.out.printf("Message received: %s%n", message.valueAsString());
					done.countDown();
				}
			}
		});
		UUID mqtt = system.addActor(() -> new MQTTResourceActor("mqtt", "tcp://localhost:1883") {
			@Override
			public void configure(MqttConnectOptions connectOptions) {
				/*
				connectOptions.setUserName("...");
				connectOptions.setPassword("...".toCharArray());
				*/
				connectOptions.setCleanSession(false);
			}

			@Override
			public MqttCallback callback() {
				return new MqttCallback() {
					@Override
					public void connectionLost(Throwable cause) {
						// empty
					}

					@Override
					public void messageArrived(String topic, MqttMessage message) throws Exception {
						tell(new String(message.getPayload(), "UTF-8"), PUBLISH, receiver);
					}

					@Override
					public void deliveryComplete(IMqttDeliveryToken token) {
						// empty
					}
				};
			}

			@Override
			public UUID clientId() {
				return UUID.fromString("470ceda2-219a-4d6d-997a-88c97a501a9b");
			}
		});
		
		system.send(new ActorMessage<>("MyTopic", SUBSCRIBE, receiver, mqtt));
		system.send(new ActorMessage<>(new MQTTPublish("MyTopic", "Hello World!".getBytes(), 2, false), PUBLISH, system.SYSTEM_ID, mqtt));
		system.send(new ActorMessage<>(new MQTTPublish("MyTopic", "Hello World Again!".getBytes(), 2, false), PUBLISH, system.SYSTEM_ID, mqtt));
		
		system.start();
		
		try {
			done.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		system.shutdownWithActors(true);
	}

	public static void main(String[] args) {
		new ExampleMQTT();
	}
}
