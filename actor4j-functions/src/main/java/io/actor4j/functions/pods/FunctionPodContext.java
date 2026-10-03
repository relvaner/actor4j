/*
 * Copyright (c) 2015-2026, David A. Bauer. All rights reserved.
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
package io.actor4j.functions.pods;

import io.actor4j.core.actors.ActorRef;
import io.actor4j.core.pods.PodContext;
import io.actor4j.functions.query.AsyncQueryRequest;
import io.actor4j.functions.state.StateStore;
import io.actor4j.functions.streams.AsyncStreams;

public class FunctionPodContext {
	protected final ActorRef host;
	protected final PodContext podContext;
	
	protected StateStore stateStore;
	protected AsyncStreams streams;
	protected AsyncQueryRequest queryRequest;
	
	public FunctionPodContext(ActorRef host, PodContext podContext) {
		super();
		this.host = host;
		this.podContext = podContext;
	}
	
	public static FunctionPodContext create(ActorRef host, PodContext podContext) {
		return new FunctionPodContext(host, podContext);
	}
	
	public ActorRef host() {
		return host;
	}
	
	public PodContext podContext() {
		return podContext;
	}
	
	public void injectStateStore(StateStore stateStore) {
		this.stateStore = stateStore;
	}
	
	public void injectStreams(AsyncStreams streams) {
		this.streams = streams;
	}
	
	public void injectQueryRequest(AsyncQueryRequest queryRequest) {
		this.queryRequest = queryRequest;
	}
	
	public StateStore stateStore() {
		return stateStore;
	}
	
	public AsyncStreams streams() {
		return streams;
	}
	
	public AsyncQueryRequest queryRequest() {
		return queryRequest;
	}
}
