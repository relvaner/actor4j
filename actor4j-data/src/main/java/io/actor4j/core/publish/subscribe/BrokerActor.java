/*
 * Copyright (c) 2015-2017, David A. Bauer. All rights reserved.
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
package io.actor4j.core.publish.subscribe;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import io.actor4j.core.actors.Actor;
import io.actor4j.core.messages.ActorMessage;
import static io.actor4j.core.messages.ActorReservedTag.*;

/**
 * publish over broker or directly to the topic actor (Tag=GET_TOPIC_ACTOR), watch(TOPIC_ACTOR) on starvation (no subscribers)
 * subscribe && unsubscribe only over the broker
 */
public class BrokerActor extends Actor {
	protected Map<String, UUID> topics;
	protected Map<String, Integer> counter;
	
	public static final int GET_TOPIC_ACTOR = RESERVED_PUBSUB_GET_TOPIC_ACTOR;
	public static final int CLEAN_UP 		= RESERVED_PUBSUB_CLEAN_UP;
	protected static final int INTERNAL_FORWARDED_BY_BROKER = RESERVED_PUBSUB_FORWARDED_BY_BROKER;
	
	public BrokerActor() {
		this("broker-actor");
	}
	
	public BrokerActor(String name) {
		super(name);
		
		topics = new HashMap<>();
		counter = new HashMap<>();
	}
	
	@Override
	public void receive(ActorMessage<?> message) {
		if (message.value()!=null) {
			if (message.value() instanceof Topic) {
				int tag = message.tag();
				
				final String topic = ((Topic)message.value()).topic();
				UUID dest = topics.get(topic);
				if (dest==null) {
					if (message.value() instanceof Unsubscribe)
						return; // Abort, the topic was not found
					dest = addChild(() -> new TopicActor("topic-actor:"+topic, topic));
					topics.put(topic, dest);
					counter.put(topic, 0);
				}
				if (message.value() instanceof Publish) {
					if (message.tag()==GET_TOPIC_ACTOR)
						tell(dest, message.tag(), message.source());
				}
				else if (message.value() instanceof Subscribe) {
					tag = INTERNAL_FORWARDED_BY_BROKER;
					counter.put(topic, counter.get(topic)+1);
				}
				else if (message.value() instanceof Unsubscribe) {
					tag = INTERNAL_FORWARDED_BY_BROKER;
					int count = counter.get(topic);
					if (count-1<=0) {
						topics.remove(topic);
						counter.remove(topic);
					}
					else
						counter.put(topic, count-1);
				}
				forward(message.shallowCopy(tag), dest);
			}
			else
				unhandled(message);
		}
		else if (message.tag()==CLEAN_UP) {
			Iterator<Entry<String, Integer>> iterator = counter.entrySet().iterator();
			while (iterator.hasNext())
				if (iterator.next().getValue()==0)
					iterator.remove();
		}
		else
			unhandled(message);
	}
}
