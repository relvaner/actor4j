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
package io.actor4j.functions.pods.runtime;

import io.actor4j.core.actors.ActorRef;
import io.actor4j.core.pods.PodContext;
import io.actor4j.functions.io.AsyncIORequest;
import io.actor4j.functions.query.AsyncQueryRequest;
import io.actor4j.functions.state.StateStore;
import io.actor4j.functions.streams.AsyncStreams;

public class FunctionPodContextImpl implements FunctionPodContextInternal {
	protected final ActorRef host;
	protected final PodContext podContext;
	
	protected StateStore<?, ?> stateStore;
	protected AsyncStreams streams;
	protected AsyncQueryRequest queryRequest;
	protected AsyncIORequest ioRequest;
	
	public FunctionPodContextImpl(ActorRef host, PodContext podContext) {
		super();
		this.host = host;
		this.podContext = podContext;
	}
	
	public static FunctionPodContextImpl create(ActorRef host, PodContext podContext) {
		return new FunctionPodContextImpl(host, podContext);
	}
	
	@Override
	public ActorRef host() {
		return host;
	}
	
	@Override
	public PodContext podContext() {
		return podContext;
	}
	
	@Override
	public void injectStateStore(StateStore<?, ?> stateStore) {
		this.stateStore = stateStore;
	}
	
	@Override
	public void injectStreams(AsyncStreams streams) {
		this.streams = streams;
	}
	
	@Override
	public void injectQueryRequest(AsyncQueryRequest queryRequest) {
		this.queryRequest = queryRequest;
	}
	
	@Override
	public void injectIORequest(AsyncIORequest ioRequest) {
		this.ioRequest = ioRequest;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <K, V> StateStore<K, V> stateStore() {
		return (StateStore<K, V>)stateStore;
	}
	
	@Override
	public AsyncStreams streams() {
		return streams;
	}
	
	@Override
	public AsyncQueryRequest queryRequest() {
		return queryRequest;
	}
	
	@Override
	public AsyncIORequest ioRequest() {
		return ioRequest;
	}
}
