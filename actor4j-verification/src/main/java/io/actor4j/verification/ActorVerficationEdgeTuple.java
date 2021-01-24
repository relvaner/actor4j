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
package io.actor4j.verification;

import java.util.List;
import java.util.Set;

public class ActorVerficationEdgeTuple {
	protected Set<Integer> events;
	protected List<String> aliases;
	
	public ActorVerficationEdgeTuple(Set<Integer> events, List<String> aliases) {
		super();
		this.events = events;
		this.aliases = aliases;
	}

	public Set<Integer> getEvents() {
		return events;
	}

	public void setEvents(Set<Integer> events) {
		this.events = events;
	}

	public List<String> getAliases() {
		return aliases;
	}

	public void setAliases(List<String> aliases) {
		this.aliases = aliases;
	}

	@Override
	public String toString() {
		return "ActorVerficationEdgeTuple [events=" + events + ", aliases=" + aliases + "]";
	}
}
