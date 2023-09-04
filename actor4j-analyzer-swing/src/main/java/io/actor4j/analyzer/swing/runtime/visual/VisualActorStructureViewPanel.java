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
package io.actor4j.analyzer.swing.runtime.visual;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingConstants;

import java.util.UUID;

import com.mxgraph.layout.mxFastOrganicLayout;
import com.mxgraph.layout.mxGraphLayout;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;

import io.actor4j.analyzer.runtime.VisualActorStructureView;
import io.actor4j.core.runtime.InternalActorCell;
import io.actor4j.core.runtime.InternalActorSystem;

public class VisualActorStructureViewPanel extends VisualActorViewPanel {
	protected static final long serialVersionUID = -1192782222987329027L;
	
	protected final VisualActorStructureView visualActorStructureView;

	protected int lastLayoutIndex;
	
	public static final AtomicInteger layoutIndex = new AtomicInteger(0);

	public VisualActorStructureViewPanel(InternalActorSystem system) {
		super(system);
		
		visualActorStructureView = new VisualActorStructureView(new AtomicReference<>(system)) {
			@Override
			public void removeVertex(Object source) {
				graph.getModel().remove(source);
			}

			@Override
			public Object addVertex(String name, String color) {
				return VisualActorStructureViewPanel.this.addVertex(name, color);
			}

			@Override
			public void addEdge(String value, Object source, Object target) {
				VisualActorStructureViewPanel.this.addEdge(value, source, target);
			}

			@Override
			public void graphUpdate(Runnable runnable) {
				graph.getModel().beginUpdate();
			    try {
			    	runnable.run();
			    } finally {
					graph.getModel().endUpdate();
				}
			    graphComponent.refresh();
			}

			@Override
			public void updateStructure() {
				VisualActorStructureViewPanel.this.updateStructure();
			}
		};
		
		lastLayoutIndex = -1;
		
		add("Structure", paDesign);
	}
			
	public void analyzeStructure(Map<UUID, InternalActorCell> actorCells, boolean showDefaultRoot, boolean showRootSystem, boolean colorize) {
		visualActorStructureView.analyzeStructure(actorCells, showDefaultRoot, showRootSystem, colorize);
	}

	@Override
	public void updateStructure() {
		resetViewport();
		
		int currentLayoutIndex = layoutIndex.get();
		if (visualActorStructureView.isChanged() || lastLayoutIndex!=currentLayoutIndex) {
			mxGraphLayout layout = null;
			switch (currentLayoutIndex){
				case 0: {
					layout = new mxFastOrganicLayout(graph);
					// the higher, the more separated
					((mxFastOrganicLayout)layout).setForceConstant(40); 
					// true transforms the edges and makes them direct lines
					((mxFastOrganicLayout)layout).setDisableEdgeStyle(false); 	
					break;
				}
				case 1: {
					layout = new mxHierarchicalLayout(graph);
					((mxHierarchicalLayout)layout).setFineTuning(true);
					((mxHierarchicalLayout)layout).setDisableEdgeStyle(false);
					break;
				}
				case 2: {
					layout = new mxHierarchicalLayout(graph, SwingConstants.WEST);
					((mxHierarchicalLayout)layout).setFineTuning(true);
					((mxHierarchicalLayout)layout).setDisableEdgeStyle(false);
					break;
				}
			}
			layout.execute(graph.getDefaultParent());
			lastLayoutIndex = currentLayoutIndex;
		}
	    
	    fitViewport();
	}
}
