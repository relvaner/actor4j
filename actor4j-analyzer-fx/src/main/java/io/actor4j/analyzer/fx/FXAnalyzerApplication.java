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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.actor4j.analyzer.VisualActorAnalyzer;
import io.actor4j.analyzer.fx.runtime.visual.FXAbstractApplication;
import io.actor4j.analyzer.fx.runtime.visual.FXVisualActorAnalyzer;
import io.actor4j.analyzer.fx.runtime.visual.FXVisualActorStage;
import io.actor4j.core.runtime.InternalActorCell;
import io.actor4j.core.runtime.InternalActorSystem;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class FXAnalyzerApplication extends FXAbstractApplication {
	protected final AtomicReference<InternalActorSystem> system;
	
	protected FXVisualActorStage visualStage;
	
	protected BorderPane borderPane;
	protected Scene scene;
	
	protected final VisualActorAnalyzer visualActorAnalyzer;
	
	protected final AtomicBoolean started;
	
	public FXAnalyzerApplication() {
		super();
		
		started = new AtomicBoolean(false);
		
		visualActorAnalyzer =  new FXVisualActorAnalyzer() {
			@Override
			public void setSystem(InternalActorSystem s) {
				super.setSystem(s);
				FXAnalyzerApplication.this.system.set(s);
			}
			
			@Override
			public void analyzeStructure(Map<UUID, InternalActorCell> actorCells, boolean showDefaultRoot,
					boolean showRootSystem, boolean colorize) {
				FXAnalyzerApplication.this.analyzeStructure(actorCells, showDefaultRoot, showRootSystem, colorize);
			}

			@Override
			public String analyzeBehaviour(Map<UUID, InternalActorCell> actorCells,
					Map<UUID, Map<UUID, Long>> deliveryRoutes, boolean showRootSystem, boolean colorize) {
				return FXAnalyzerApplication.this.analyzeBehaviour(actorCells, deliveryRoutes, showRootSystem, colorize);
			}

			@Override
			public void setStatus(String text) {
				FXAnalyzerApplication.this.setStatus(text);
			}
		};
		
		system = new AtomicReference<>();
	}

	@Override
	public String getTitle() {
		return "actor4j-analyzer (Structure & Behaviour)";
	}

	@Override
	public void run(Stage primaryStage) {
		borderPane = new BorderPane();
		visualStage = new FXVisualActorStage(borderPane, system);

		scene = new Scene(borderPane, 1280, 800);
//		scene.getStylesheets().add(getClass().getClassLoader().getResource("css/file.css").toExternalForm());
        primaryStage.setScene(scene);
	}
	
	@Override
	public void afterShow() {
		started.set(true);
		
		visualStage.afterShow();
	}
	
	public VisualActorAnalyzer getVisualActorAnalyzer() {
		return visualActorAnalyzer;
	}
	
	public void analyzeStructure(Map<UUID, InternalActorCell> actorCells, boolean showDefaultRoot, boolean showRootSystem, boolean colorize) {
		if (started.get())
			visualStage.analyzeStructure(actorCells, showDefaultRoot, showRootSystem, colorize);
	}
	
	public String analyzeBehaviour(Map<UUID, InternalActorCell> actorCells, Map<UUID, Map<UUID, Long>> deliveryRoutes, boolean showRootSystem, boolean colorize) {
		String result = null;
		
		if (started.get())
			result = visualStage.analyzeBehaviour(actorCells, deliveryRoutes, showRootSystem, colorize);
		
		return result;
	}
	
	public void setStatus(String text) {
		if (started.get())
			visualStage.setStatus(text);
	}
}
