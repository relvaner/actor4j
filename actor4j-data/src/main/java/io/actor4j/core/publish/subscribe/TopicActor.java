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

import io.actor4j.core.actors.Actor;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.HubPattern;

public class TopicActor extends Actor {
	protected String topic;
	protected HubPattern hub; 
	
	public TopicActor(String name, String topic) {
		super(name);
		this.topic = topic;
		
		hub = new HubPattern(this);
	}
	
	@Override
	public void receive(ActorMessage<?> message) {
		if (message.value!=null) {
			String buf = ((Topic)message.value).topic;
			if (topic.equals(buf)) {
				if (message.value instanceof Publish)
					hub.broadcast(message);
				else if (message.value instanceof Subscribe && message.tag==BrokerActor.INTERNAL_FORWARDED_BY_BROKER)
					hub.add(message.source);
				else if (message.value instanceof Unsubscribe && message.tag==BrokerActor.INTERNAL_FORWARDED_BY_BROKER) {
					hub.remove(message.source);
					if (hub.count()==0)
						stop();
				}
			}
			else
				unhandled(message);
		}
		else
			unhandled(message);
	}
}
