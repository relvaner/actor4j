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
package io.actor4j.patterns.actors;

import java.util.function.Predicate;

import io.actor4j.core.id.ActorId;
import io.actor4j.core.messages.ActorMessage;

public class RouteeHandler {
	protected Predicate<ActorMessage<?>> predicate;
	protected ActorId routee;
	
	public RouteeHandler(Predicate<ActorMessage<?>> predicate, ActorId routee) {
		super();
		this.predicate = predicate;
		this.routee = routee;
	}

	public Predicate<ActorMessage<?>> getPredicate() {
		return predicate;
	}
	
	public void setPredicate(Predicate<ActorMessage<?>> predicate) {
		this.predicate = predicate;
	}
	
	public ActorId getRoutee() {
		return routee;
	}
	
	public void setRoutee(ActorId routee) {
		this.routee = routee;
	}
}
