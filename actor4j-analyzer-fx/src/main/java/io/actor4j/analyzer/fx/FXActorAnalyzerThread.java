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
package io.actor4j.analyzer.fx;

import java.util.Map;
import java.util.UUID;

import io.actor4j.analyzer.BaseActorAnalyzerThread;
import io.actor4j.analyzer.VisualActorAnalyzer;
import io.actor4j.core.runtime.InternalActorCell;
import javafx.application.Platform;

public class FXActorAnalyzerThread extends BaseActorAnalyzerThread {
	public FXActorAnalyzerThread(long delay, boolean showDefaultRoot, boolean showRootSystem, boolean colorize,
			VisualActorAnalyzer visualAnalyzer) {
		super(delay, showDefaultRoot, showRootSystem, colorize, visualAnalyzer);
	}

	public FXActorAnalyzerThread(long delay, boolean showDefaultRoot, boolean showRootSystem,
			VisualActorAnalyzer visualAnalyzer) {
		super(delay, showDefaultRoot, showRootSystem, visualAnalyzer);
	}

	public FXActorAnalyzerThread(long delay, boolean showDefaultRoot, VisualActorAnalyzer visualAnalyzer) {
		super(delay, showDefaultRoot, visualAnalyzer);
	}

	@Override
	protected void update(final Map<UUID, InternalActorCell> cells) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				visualAnalyzer.analyzeStructure(cells, showDefaultRoot, showRootSystem, colorize);
				String status = visualAnalyzer.analyzeBehaviour(cells, deliveryRoutes, showRootSystem, colorize);
				visualAnalyzer.setStatus(status);
			}
		});
	}
}
