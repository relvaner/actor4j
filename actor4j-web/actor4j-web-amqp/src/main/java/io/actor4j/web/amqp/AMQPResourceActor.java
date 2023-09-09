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
package io.actor4j.web.amqp;

import static io.actor4j.core.logging.ActorLogger.logger;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;

import io.actor4j.core.actors.ResourceActor;
import io.actor4j.core.messages.ActorMessage;

/*
 *  MQTT compatible implementation (MQTT topics use a topic exchange (amq.topic by default))
 * 	@See https://www.rabbitmq.com/mqtt.html 
 */
public abstract class AMQPResourceActor extends ResourceActor {
	public static final int PUBLISH     = 1;
	public static final int SUBSCRIBE   = 2;
	public static final int UNSUBSCRIBE = 3;
	
	protected volatile Connection connection;
	protected final String host;
	protected final int port;
	protected final Map<String, String> consumerTags;
	protected final Lock lock;
	
	public AMQPResourceActor(String host, int port) {
		this(null, host, port);
	}

	public AMQPResourceActor(String name, String host, int port) {
		super(name);
		this.host = host;
		this.port = port;
		
		consumerTags = new ConcurrentHashMap<>();
		lock = new ReentrantLock();
	}
	
	public abstract void configure(ConnectionFactory factory);
	public abstract Consumer callback(Channel channel);

	@Override
	public void preStart() {
		super.preStart();
		
		ConnectionFactory factory = new ConnectionFactory();
	    factory.setHost(host);
	    factory.setPort(port);
	    configure(factory);
		try {
			connection = factory.newConnection();
			logger().info(String.format("%s - AMQP-Service started...", name));
		} catch (IOException | TimeoutException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void postStop() {
		try {
			connection.close();
			logger().info(String.format("%s - AMQP-Service stopped...", name));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void receive(ActorMessage<?> message) {
		if (message.tag()==PUBLISH && message.value()!=null && message.value() instanceof AMQPPublish)
			publish((AMQPPublish)message.value());
		else if (message.tag()==SUBSCRIBE && message.value()!=null && message.value() instanceof String)
			subscribe(message.valueAsString());
		else if (message.tag()==UNSUBSCRIBE && message.value()!=null && message.value() instanceof String)
			unsubscribe(message.valueAsString());
		else
			unhandled(message);
	}
	
	public void publish(AMQPPublish publish) {
		try {
			Channel channel = connection.createChannel();
			channel.basicPublish("amq.topic", publish.topic(), null, publish.payload());
			channel.close();
		} catch (IOException | TimeoutException e) {
			e.printStackTrace();
		}
	}
	
	public void subscribe(String topicFilter) {
		lock.lock();
		try {
			String consumerTag = consumerTags.get(topicFilter);
			if (consumerTag==null)
				try {
					Channel channel = connection.createChannel();
					String queueName = channel.queueDeclare().getQueue();
					channel.queueBind(queueName, "amq.topic", topicFilter);
					consumerTag = channel.basicConsume(queueName, true, callback(channel));
					consumerTags.put(topicFilter, consumerTag);
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		finally {
			lock.unlock();
		}
		
	}
	
	public void unsubscribe(String topicFilter) {
		lock.lock();
		try {
			String consumerTag = consumerTags.get(topicFilter);
			if (consumerTag!=null)
				try {
					Channel channel = connection.createChannel();
					channel.basicCancel(consumerTag);
					channel.close();
				} catch (IOException | TimeoutException e) {
					e.printStackTrace();
				}
		}
		finally {
			lock.unlock();
		}
	}
}
