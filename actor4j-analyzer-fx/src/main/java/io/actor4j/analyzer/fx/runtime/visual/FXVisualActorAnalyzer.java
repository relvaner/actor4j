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
package io.actor4j.analyzer.fx.runtime.visual;

import java.util.Map;
import java.util.UUID;

import io.actor4j.analyzer.VisualActorAnalyzer;
import io.actor4j.core.runtime.InternalActorCell;
import io.actor4j.core.runtime.InternalActorSystem;

public abstract class FXVisualActorAnalyzer implements VisualActorAnalyzer {
	protected InternalActorSystem system;
	
	@Override
	public InternalActorSystem getSytem() {
		return system;
	}
	
	@Override
	public void setSystem(InternalActorSystem system) {
		this.system = system;
	}

	@Override
	public void start() {
		// empty
	}

	@Override
	public void stop() {
		// empty
	}

	public abstract void analyzeStructure(Map<UUID, InternalActorCell> actorCells, boolean showDefaultRoot, boolean showRootSystem, boolean colorize);
	public abstract String analyzeBehaviour(Map<UUID, InternalActorCell> actorCells, Map<UUID, Map<UUID, Long>> deliveryRoutes, boolean showRootSystem, boolean colorize);
	
	public abstract void setStatus(String text);
}
