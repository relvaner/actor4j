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
package io.actor4j.core.publish.subscribe.utils;

import static io.actor4j.core.publish.subscribe.BrokerActor.*;

import java.util.UUID;

import io.actor4j.core.actors.ActorRef;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorFactory;
import io.actor4j.core.utils.ActorOptional;
import io.actor4j.core.publish.subscribe.BrokerActor;
import io.actor4j.core.publish.subscribe.Publish;
import io.actor4j.core.publish.subscribe.Subscribe;
import io.actor4j.core.publish.subscribe.Unsubscribe;

public class PubSubActorManager<T> {
	protected ActorRef actorRef;
	protected UUID broker;
	
	public PubSubActorManager(ActorRef actorRef, UUID broker) {
		this.actorRef = actorRef;
		this.broker = broker;
	}

	public void createBroker() {
		broker = actorRef.getSystem().addActor(() -> new BrokerActor());
	}
	
	public static ActorFactory createBrokerWithFactory() {
		return () -> new BrokerActor();
	}
	
	@SuppressWarnings("unchecked")
	public ActorOptional<T> get(ActorMessage<?> message) {
		if (message.value()!=null && message.value() instanceof Publish)
			return ActorOptional.of(((Publish<T>)message.value()).value());
		else
			return ActorOptional.none();
	}
	
	public ActorOptional<UUID> getTopic(ActorMessage<?> message) {
		if (message.source().equals(broker) && message.tag()==GET_TOPIC_ACTOR)
			return ActorOptional.of(message.valueAsUUID());
		else
			return ActorOptional.none();
	}
	
	public void publish(Publish<T> value) {
		actorRef.send(ActorMessage.create(value, GET_TOPIC_ACTOR, actorRef.getId(), broker));
	}
	
	public void publish(Publish<T> value, UUID topic) {
		actorRef.send(ActorMessage.create(value, 0, actorRef.getId(), topic));
	}
	
	public void subscribe(String topic) {
		actorRef.send(ActorMessage.create(new Subscribe(topic), 0, actorRef.getId(), broker));
	}
	
	public void unsubscribe(String topic) {
		actorRef.send(ActorMessage.create(new Unsubscribe(topic), 0, actorRef.getId(), broker));
	}
}
