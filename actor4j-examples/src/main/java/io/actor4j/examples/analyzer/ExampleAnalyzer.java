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
package io.actor4j.examples.analyzer;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.actor4j.core.ActorSystem;
import io.actor4j.core.actors.Actor;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorFactory;
import io.actor4j.core.utils.ActorGroup;
import io.actor4j.core.utils.ActorGroupSet;
import io.actor4j.core.utils.HubPattern;
import io.actor4j.analyzer.ActorAnalyzer;
import io.actor4j.analyzer.DefaultActorAnalyzerThread;

public class ExampleAnalyzer {
	public ExampleAnalyzer() {
		ActorSystem system = ActorAnalyzer.create(new DefaultActorAnalyzerThread(2000, true));

		final int size = 2;
		ActorGroup group = new ActorGroupSet();
		for (int i=0; i<size; i++) {
			final int f_i = i;
			UUID id = system.addActor(new ActorFactory() {
				@Override
				public Actor create() {
					return new Actor("group-"+f_i) {
						protected boolean first = true;
						protected UUID last;
						@Override
						public void receive(ActorMessage<?> message) {
							if (first) {
								UUID next = self();
								for (int i=0; i<3; i++) {
									final int f_i = i;
									final UUID f_next = next;
									UUID current = addChild(() -> new Sender("child-"+f_i, f_next));
									next = current;
								}
								last = next;
								first = false;
							}
							if (message.tag()==1)
								send(ActorMessage.create(null, 0, self(), last));
						}
					};
				}
			});
			group.add(id);
		}
		UUID id = system.addActor(new ActorFactory() {
			@Override
			public Actor create() {
				return new Actor("group-"+size) {
					protected HubPattern hub = new HubPattern(this);
					protected boolean first = true;
					@Override
					public void receive(ActorMessage<?> message) {
						if (first) {
							for (int i=0; i<4; i++) {
								final int f_i = i;
								UUID childId = addChild(new ActorFactory() {
									@Override
									public Actor create() {
										return new Actor("child-"+f_i){
											@Override
											public void receive(ActorMessage<?> message) {
											}
										};
									}
								});
								hub.add(childId);
							}
							first = false;
						}
						hub.broadcast(ActorMessage.create(null, 0, self(), null));
					}
				};
			}
		});
		group.add(id);
		
		UUID ping = system.addActor(new ActorFactory() {
			@Override
			public Actor create() {
				return new Actor("ping") {
					protected boolean first = true;
					protected UUID pong;
					@Override
					public void receive(ActorMessage<?> message) {
						if (first) {
							pong = addChild(new ActorFactory() {
								@Override
								public Actor create() {
									return new Actor("pong") {
										@Override
										public void receive(ActorMessage<?> message) {
											send(message.shallowCopy(message.dest(), message.source()));
										}
									};
								}
							});
							first = false;
						}
						if (message.tag()==1)
							send(ActorMessage.create(null, 0, self(), pong));
					}
				};
			}
		});
		group.add(ping);

		system
			.start();
		
		system.timer().schedule(ActorMessage.create(null, 1, system.SYSTEM_ID(), null), group, 0, 500, TimeUnit.MILLISECONDS);
		system.timer().scheduleOnce(ActorMessage.create(null, Actor.RESTART, system.SYSTEM_ID(), null), ping, 5, TimeUnit.SECONDS);
		system.timer().scheduleOnce(ActorMessage.create(null, Actor.STOP, system.SYSTEM_ID(), null), id, 15, TimeUnit.SECONDS);
		system.timer().scheduleOnce(ActorMessage.create(null, Actor.STOP, system.SYSTEM_ID(), null), system.USER_ID(), 25, TimeUnit.SECONDS);
		
		try {
			Thread.sleep(240000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		system.shutdownWithActors(true);
	}

	public static void main(String[] args) {
		new ExampleAnalyzer();
	}
}
