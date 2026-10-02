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

import io.actor4j.core.pods.PodContext;
import io.actor4j.functions.query.QueryRequest;
import io.actor4j.functions.state.StateStore;
import io.actor4j.functions.streams.Streams;

public class FunctionPodContext {
	protected final PodContext podContext;
	
	protected StateStore stateStore;
	protected Streams streams;
	protected QueryRequest queryRequest;
	
	public FunctionPodContext(PodContext podContext) {
		super();
		this.podContext = podContext;
	}
	
	public static FunctionPodContext create(PodContext podContext) {
		return new FunctionPodContext(podContext);
	}
	
	public PodContext podContext() {
		return podContext;
	}
	
	public void injectStateStore(StateStore stateStore) {
		this.stateStore = stateStore;
	}
	
	public void injectStreams(Streams streams) {
		this.streams = streams;
	}
	
	public void injectQueryRequest(QueryRequest queryRequest) {
		this.queryRequest = queryRequest;
	}
	
	public StateStore stateStore() {
		return stateStore;
	}
	
	public Streams streams() {
		return streams;
	}
	
	public QueryRequest queryRequest() {
		return queryRequest;
	}
}
