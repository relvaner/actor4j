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
package io.actor4j.examples.amqp;

import static io.actor4j.web.amqp.AMQPResourceActor.*;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import io.actor4j.core.ActorSystem;
import io.actor4j.core.actors.Actor;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.web.amqp.AMQPPublish;
import io.actor4j.web.amqp.AMQPResourceActor;

public class ExampleAMQP {
	public ExampleAMQP() {
		super();
		
		ActorSystem system = new ActorSystem();
		
		CountDownLatch done = new CountDownLatch(2);
		
		UUID receiver = system.addActor(() -> new Actor("receiver") {
			@Override
			public void receive(ActorMessage<?> message) {
				if (message.tag()==PUBLISH && message.value()!=null) {
					System.out.printf("Message received: %s%n", message.valueAsString());
					done.countDown();
				}
			}
		});
		UUID amqp = system.addActor(() -> new AMQPResourceActor("amqp", "localhost", 5672) {
			@Override
			public void configure(ConnectionFactory factory) {
				/*
				factory.setUsername("...");
				factory.setPassword("...");
				*/
			}
			
			@Override
			public Consumer callback(Channel channel) {
				return new DefaultConsumer(channel) {
					@Override
					public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties,
							byte[] body) throws IOException {
						tell(new String(body, "UTF-8"), PUBLISH, receiver);
					}
				};
			}
		});
		
		system.send(ActorMessage.create("MyTopic", SUBSCRIBE, receiver, amqp));
		system.send(ActorMessage.create(new AMQPPublish("MyTopic", "Hello World!".getBytes()), PUBLISH, system.SYSTEM_ID, amqp));
		system.send(ActorMessage.create(new AMQPPublish("MyTopic", "Hello World Again!".getBytes()), PUBLISH, system.SYSTEM_ID, amqp));
		
		system.start();
		
		try {
			done.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		system.shutdownWithActors(true);
	}

	public static void main(String[] args) {
		new ExampleAMQP();
	}
}
