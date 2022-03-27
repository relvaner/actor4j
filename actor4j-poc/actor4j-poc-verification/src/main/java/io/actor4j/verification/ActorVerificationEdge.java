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

import org.jgrapht.graph.DefaultEdge;

public class ActorVerificationEdge extends DefaultEdge {
	protected static final long serialVersionUID = -3962581299999193897L;
	
	protected Set<Integer> events;
	protected List<ActorVerficationEdgeTuple> tuples;
	
	public ActorVerificationEdge(Set<Integer> events, List<ActorVerficationEdgeTuple> tuples) {
		super();
		this.events = events;
		this.tuples = tuples;
	}

	public Set<Integer> getEvents() {
		return events;
	}

	public List<ActorVerficationEdgeTuple> getTuples() {
		return tuples;
	}
	
	@Override
	protected String getSource() {
		return super.getSource().toString();
	}
	
	@Override
	protected String getTarget() {
		return super.getTarget().toString();
	}

	@Override
	public String toString() {
		return "ActorVerificationEdge [events=" + events + ", tuples=" + tuples + ", source=" + getSource() + ", target="
				+ getTarget() + "]";
	}
}
