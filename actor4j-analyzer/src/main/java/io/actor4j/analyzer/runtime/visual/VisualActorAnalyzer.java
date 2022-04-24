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
package io.actor4j.analyzer.runtime.visual;

import java.awt.event.WindowEvent;
import java.util.Map;
import java.util.UUID;

import io.actor4j.core.runtime.InternalActorCell;
import io.actor4j.core.runtime.InternalActorSystem;
import tools4j.utils.SwingSubApplication;

public class VisualActorAnalyzer {
	protected InternalActorSystem system;
	protected SwingSubApplication application;
	
	public VisualActorAnalyzer(InternalActorSystem system) {
		super();
		
		this.system = system;
	}
	
	public void start() {
		application = new SwingSubApplication();
		application.setTitle("actor4j-analyzer (Structure & Behaviour)");
		application.runApplication(new VisualActorFrame(system));
	}
	
	public void stop() {
		application.getFrame().dispatchEvent(new WindowEvent(application.getFrame(), WindowEvent.WINDOW_CLOSING));
	}
	
	public void analyzeStructure(Map<UUID, InternalActorCell> actorCells, boolean showDefaultRoot, boolean showRootSystem, boolean colorize) {
		((VisualActorFrame)application.getFrame()).analyzeStructure(actorCells, showDefaultRoot, showRootSystem, colorize);
	}
	
	public void analyzeBehaviour(Map<UUID, InternalActorCell> actorCells, Map<UUID, Map<UUID, Long>> deliveryRoutes, boolean showRootSystem, boolean colorize) {
		((VisualActorFrame)application.getFrame()).analyzeBehaviour(actorCells, deliveryRoutes, showRootSystem, colorize);
	}
}
