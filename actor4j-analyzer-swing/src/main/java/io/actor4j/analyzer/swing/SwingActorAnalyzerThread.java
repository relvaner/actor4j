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
package io.actor4j.analyzer.swing;

import java.util.Set;

import javax.swing.SwingUtilities;

import io.actor4j.analyzer.BaseActorAnalyzerThread;
import io.actor4j.analyzer.VisualActorAnalyzer;
import io.actor4j.core.runtime.InternalActorCell;

public class SwingActorAnalyzerThread extends BaseActorAnalyzerThread {
	public SwingActorAnalyzerThread(long delay, boolean showDefaultRoot, boolean showRootSystem, boolean colorize,
			VisualActorAnalyzer visualAnalyzer) {
		super(delay, showDefaultRoot, showRootSystem, colorize, visualAnalyzer);
	}

	public SwingActorAnalyzerThread(long delay, boolean showDefaultRoot, boolean showRootSystem,
			VisualActorAnalyzer visualAnalyzer) {
		super(delay, showDefaultRoot, showRootSystem, visualAnalyzer);
	}

	public SwingActorAnalyzerThread(long delay, boolean showDefaultRoot, VisualActorAnalyzer visualAnalyzer) {
		super(delay, showDefaultRoot, visualAnalyzer);
	}

	@Override
	protected void update(final Set<InternalActorCell> actorCells) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				visualAnalyzer.analyzeStructure(actorCells, showDefaultRoot, showRootSystem, colorize);
				String status = visualAnalyzer.analyzeBehaviour(actorCells, deliveryRoutes, showRootSystem, colorize);
				visualAnalyzer.setStatus(status);
			}
		});
	}
}
