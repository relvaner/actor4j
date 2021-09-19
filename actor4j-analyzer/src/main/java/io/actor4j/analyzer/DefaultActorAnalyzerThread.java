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
package io.actor4j.analyzer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.SwingUtilities;

import io.actor4j.analyzer.internal.ActorAnalyzerThread;
import io.actor4j.analyzer.internal.visual.VisualActorAnalyzer;
import io.actor4j.core.internal.ActorCell;
import io.actor4j.core.internal.ActorSystemImpl;
import io.actor4j.core.messages.ActorMessage;

public class DefaultActorAnalyzerThread extends ActorAnalyzerThread {
	protected VisualActorAnalyzer visualAnalyzer;
	
	protected Map<UUID, Map<UUID, Long>> deliveryRoutes;
	
	protected boolean showDefaultRoot;
	protected boolean showRootSystem;
	protected boolean colorize;
	
	public DefaultActorAnalyzerThread(long delay, boolean showDefaultRoot) {
		this(delay, showDefaultRoot, false, false);
	}
	
	public DefaultActorAnalyzerThread(long delay, boolean showDefaultRoot, boolean showRootSystem) {
		this(delay, showDefaultRoot, showRootSystem, false);
	}
	
	public DefaultActorAnalyzerThread(long delay, boolean showDefaultRoot, boolean showRootSystem, boolean colorize) {
		super(delay);
		
		this.showDefaultRoot = showDefaultRoot;
		this.showRootSystem = showRootSystem;
		this.colorize = colorize;
		
		deliveryRoutes = new ConcurrentHashMap<>();
	}
	
	@Override
	protected void setSystem(ActorSystemImpl system) {
		super.setSystem(system);
		
		visualAnalyzer = new VisualActorAnalyzer(system);
	}
	
	@Override
	protected void analyze(ActorMessage<?> message) {
		if (message.source==null)
			message.source = system.UNKNOWN_ID;
		if (message.dest==null)
			message.dest = system.UNKNOWN_ID;
		
		Map<UUID, Long> routes = deliveryRoutes.get(message.source);
		if (routes==null) {
			routes = new ConcurrentHashMap<>();
			deliveryRoutes.put(message.source, routes);
		}
		Long count = routes.get(message.dest);
		if (count==null)
			routes.put(message.dest, 1L);
		else
			routes.put(message.dest, count+1);
	}

	@Override
	protected void update(final Map<UUID, ActorCell> cells) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				visualAnalyzer.analyzeStructure(cells, showDefaultRoot, showRootSystem, colorize);
				visualAnalyzer.analyzeBehaviour(cells, deliveryRoutes, showRootSystem, colorize);
			}
		});
	}
	
	@Override
	public void run() {
		visualAnalyzer.start();
		
		super.run();
		
		visualAnalyzer.stop();
	}
}
