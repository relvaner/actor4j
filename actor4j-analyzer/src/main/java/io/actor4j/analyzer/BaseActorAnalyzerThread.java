/*
 * Copyright (c) 2015-2023, David A. Bauer. All rights reserved.
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
package io.actor4j.analyzer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.actor4j.analyzer.runtime.ActorAnalyzerThread;
import io.actor4j.core.runtime.InternalActorSystem;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.messages.ActorMessage;

public abstract class BaseActorAnalyzerThread extends ActorAnalyzerThread {
	protected final VisualActorAnalyzer visualAnalyzer;
	
	protected final Map<ActorId, Map<ActorId, Long>> deliveryRoutes;
	
	protected final boolean showDefaultRoot;
	protected final boolean showRootSystem;
	protected final boolean colorize;
	
	public BaseActorAnalyzerThread(long delay, boolean showDefaultRoot, VisualActorAnalyzer visualAnalyzer) {
		this(delay, showDefaultRoot, false, false, visualAnalyzer);
	}
	
	public BaseActorAnalyzerThread(long delay, boolean showDefaultRoot, boolean showRootSystem, VisualActorAnalyzer visualAnalyzer) {
		this(delay, showDefaultRoot, showRootSystem, false, visualAnalyzer);
	}
	
	public BaseActorAnalyzerThread(long delay, boolean showDefaultRoot, boolean showRootSystem, boolean colorize, VisualActorAnalyzer visualAnalyzer) {
		super(delay);
		
		this.showDefaultRoot = showDefaultRoot;
		this.showRootSystem = showRootSystem;
		this.colorize = colorize;
		this.visualAnalyzer = visualAnalyzer;
		
		deliveryRoutes = new ConcurrentHashMap<>();
	}
	
	@Override
	protected void setSystem(InternalActorSystem system) {
		super.setSystem(system);
		
		visualAnalyzer.setSystem(system);
	}
	
	@Override
	protected void analyze(ActorMessage<?> message) {
		ActorId source = message.source();
		ActorId dest = message.dest();
		
		if (message.source()==null)
			source = system.UNKNOWN_ID();
		if (message.dest()==null)
			dest = system.UNKNOWN_ID();
		
		Map<ActorId, Long> routes = deliveryRoutes.get(source);
		if (routes==null) {
			routes = new ConcurrentHashMap<>();
			deliveryRoutes.put(source, routes);
		}
		Long count = routes.get(dest);
		if (count==null)
			routes.put(dest, 1L);
		else
			routes.put(dest, count+1);
	}
	
	@Override
	public void run() {
		visualAnalyzer.start();
		
		super.run();
		
		visualAnalyzer.stop();
	}
}
