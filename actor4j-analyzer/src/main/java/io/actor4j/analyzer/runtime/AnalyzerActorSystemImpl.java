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
package io.actor4j.analyzer.runtime;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import io.actor4j.analyzer.ActorAnalyzer;
import io.actor4j.analyzer.config.ActorAnalyzerConfig;
import io.actor4j.core.actors.PseudoActor;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.runtime.ActorSystemImpl;
import io.actor4j.core.runtime.DefaultActorSystemImpl;
import io.actor4j.core.runtime.InternalActorCell;

public class AnalyzerActorSystemImpl extends DefaultActorSystemImpl implements ActorAnalyzer {
	protected AtomicBoolean analyzeMode;
	protected ActorAnalyzerThread analyzerThread;
	
	protected final Set<InternalActorCell> actorCells; // current workaround
	
	private AnalyzerActorSystemImpl (ActorAnalyzerConfig config) {
		super(config!=null ? config : ActorAnalyzerConfig.create());
		
		analyzeMode = new AtomicBoolean(false);
		
		messageDispatcher = new AnalyzerActorMessageDispatcher(this);
		
		actorCells = ConcurrentHashMap.newKeySet();
		actorCells.add((InternalActorCell)USER_ID);
		actorCells.add((InternalActorCell)SYSTEM_ID);
		actorCells.add((InternalActorCell)UNKNOWN_ID);
		actorCells.add((InternalActorCell)PSEUDO_ID);
	}
	
	public AnalyzerActorSystemImpl(ActorAnalyzerThread analyzerThread, ActorAnalyzerConfig config) {
		this(config);

		analyze(analyzerThread);
	}
	
	public Set<InternalActorCell> getActorCells() {
		return actorCells;
	}
	
	@Override
	public boolean setConfig(ActorAnalyzerConfig config) {
		return super.setConfig(config);
	}
	
	@Override
	public ActorId internal_addCell(InternalActorCell cell) {
		if (actorCells!=null && !(cell.getActor() instanceof PseudoActor))
			actorCells.add(cell);
		return super.internal_addCell(cell);
	}
	
	public ActorSystemImpl analyze(ActorAnalyzerThread analyzerThread) {
		if (!executorService.isStarted()) {
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
	public boolean start(Runnable onStartup, Runnable onTermination) {
		if (!executorService.isStarted())
			if (analyzeMode.get())
				analyzerThread.start();
		return super.start(onStartup, onTermination);
	}
	
	@Override
	public void shutdownWithActors(final boolean await) {
		if (executorService.isStarted())
			if (analyzeMode.get()) {
				analyzeMode.set(false);
				analyzerThread.interrupt();
			}
		super.shutdownWithActors(await);
	}
	
	@Override
	public void shutdown(boolean await) {
		if (executorService.isStarted())
			if (analyzeMode.get()) {
				analyzeMode.set(false);
				analyzerThread.interrupt();
			}
		super.shutdown(await);
	}
}
