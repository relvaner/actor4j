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
package io.actor4j.analyzer.internal;

import java.util.concurrent.atomic.AtomicBoolean;

import io.actor4j.analyzer.config.ActorAnalyzerConfig;
import io.actor4j.core.ActorSystem;
import io.actor4j.core.internal.ActorSystemImpl;
import io.actor4j.core.internal.DefaultActorSystemImpl;

public class AnalyzerActorSystemImpl extends DefaultActorSystemImpl {
	protected AtomicBoolean analyzeMode;
	protected ActorAnalyzerThread analyzerThread;

	public AnalyzerActorSystemImpl(ActorSystem wrapper) {
		this(wrapper, null);
	}
	
	public AnalyzerActorSystemImpl (ActorSystem wrapper, ActorAnalyzerConfig config) {
		super(wrapper, config!=null ? config : ActorAnalyzerConfig.create());
		
		analyzeMode = new AtomicBoolean(false);
		
		messageDispatcher = new AnalyzerActorMessageDispatcher(this);
	}
	
	public ActorSystemImpl analyze(ActorAnalyzerThread analyzerThread) {
		if (!executerService.isStarted()) {
			this.analyzerThread = analyzerThread;
			if (analyzerThread!=null) {
				analyzerThread.setSystem(this);
				analyzeMode.set(true);
			}
		}
		
		return this;
	}
	
	public ActorAnalyzerThread getAnalyzerThread() {
		return analyzerThread;
	}

	public AtomicBoolean getAnalyzeMode() {
		return analyzeMode;
	}
	
	@Override
	public void start(Runnable onStartup, Runnable onTermination) {
		if (!executerService.isStarted())
			if (analyzeMode.get())
				analyzerThread.start();
		super.start(onStartup, onTermination);
	}
	
	@Override
	public void shutdownWithActors(final boolean await) {
		if (executerService.isStarted())
			if (analyzeMode.get()) {
				analyzeMode.set(false);
				analyzerThread.interrupt();
			}
		super.shutdownWithActors(await);
	}
	
	@Override
	public void shutdown(boolean await) {
		if (executerService.isStarted())
			if (analyzeMode.get()) {
				analyzeMode.set(false);
				analyzerThread.interrupt();
			}
		super.shutdown(await);
	}
}
