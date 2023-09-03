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
import java.util.concurrent.atomic.AtomicReference;

import io.actor4j.core.runtime.InternalActorCell;
import io.actor4j.core.runtime.InternalActorSystem;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;

public class FXVisualActorStage {
	protected final AtomicReference<InternalActorSystem> system;
	
	protected BorderPane borderPane;
	
	protected FXVisualActorMenuBar menuBar;
	
	protected TabPane leftTabPane;
	protected TabPane rightTabPane;
	
	protected SplitPane splitPane;
	
	protected FXVisualActorViewTab leftView;
	protected FXVisualActorViewTab rightView;
	
	protected Label statusBar;
	protected BorderPane statusPane;

	public FXVisualActorStage(BorderPane borderPane, AtomicReference<InternalActorSystem> system) {
		super();
		
		this.borderPane = borderPane;
		this.system = system;
		
		initialize();
	}

	public void initialize() {
//		menuBar = new FXVisualActorMenuBar();
		
		leftTabPane = new TabPane();
		leftTabPane.getStyleClass().add("bottom-tab-header");
		leftView = new FXVisualActorStructureViewTab("Structure", system);
		leftTabPane.getTabs().add(leftView);
		
		rightTabPane = new TabPane();
		rightTabPane.getStyleClass().add("bottom-tab-header");
		rightView = new FXVisualActorBehaviourViewTab("Behaviour", system);
		rightTabPane.getTabs().add(rightView);
		
		splitPane = new SplitPane(leftTabPane, rightTabPane);
		splitPane.setDividerPositions(0.5);
		
		statusBar = new Label("");
		statusPane = new BorderPane();
		statusPane.setLeft(statusBar);
		
//		borderPane.setTop(menuBar);
		borderPane.setCenter(splitPane);
		borderPane.setBottom(statusPane);
	}
	
	public void afterShow() {
		leftView.afterShow();
		rightView.afterShow();
	}
	
	public void analyzeStructure(Map<UUID, InternalActorCell> actorCells, boolean showDefaultRoot, boolean showRootSystem, boolean colorize) {
		((FXVisualActorStructureViewTab)leftView).analyzeStructure(actorCells, showDefaultRoot, showRootSystem, colorize);
	}
	
	public String analyzeBehaviour(Map<UUID, InternalActorCell> actorCells, Map<UUID, Map<UUID, Long>> deliveryRoutes, boolean showRootSystem, boolean colorize) {
		return ((FXVisualActorBehaviourViewTab)rightView).analyzeBehaviour(actorCells, deliveryRoutes, showRootSystem, colorize);
	}
	
	public void setStatus(String text) {
		statusBar.setText(text);
	}
}
