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

import java.util.concurrent.atomic.AtomicReference;

import com.brunomnsilva.smartgraph.graph.Graph;
import com.brunomnsilva.smartgraph.graph.GraphEdgeList;
import com.brunomnsilva.smartgraph.graphview.SmartGraphPanel;
import com.brunomnsilva.smartgraph.graphview.SmartGraphProperties;

import io.actor4j.core.runtime.InternalActorSystem;
import javafx.scene.control.Tab;

public class FXVisualActorViewTab extends Tab {
	protected final AtomicReference<InternalActorSystem> system;
	
	protected SmartGraphProperties graphProperties;
	protected Graph<VertexElement, EdgeElement> graph;
	protected SmartGraphPanel<VertexElement, EdgeElement> graphView;
	
	public final String customGraphProperties = "edge.label = true" + "\n" + "edge.arrow = true";
	
	public FXVisualActorViewTab(String text, AtomicReference<InternalActorSystem> system) {
		super(text);
		
		this.system = system;
		
		initialize();
	}

	public void initialize() {
        graphProperties = new SmartGraphProperties(customGraphProperties);
        graph = new GraphEdgeList<>();

        graphView = new SmartGraphPanel<>(graph, graphProperties);
        setContent(new CustomContentZoomPane(graphView));
	}
	
	public void afterShow() {
		graphView.init();
        graphView.setAutomaticLayout(true);
	}
}
