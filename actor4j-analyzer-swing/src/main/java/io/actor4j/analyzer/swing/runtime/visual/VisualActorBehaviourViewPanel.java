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
package io.actor4j.analyzer.swing.runtime.visual;

import com.mxgraph.layout.mxFastOrganicLayout;
import com.mxgraph.layout.mxGraphLayout;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.model.mxCell;

import io.actor4j.analyzer.runtime.VisualActorBehaviourView;
import io.actor4j.core.runtime.InternalActorCell;
import io.actor4j.core.runtime.InternalActorSystem;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingConstants;

public class VisualActorBehaviourViewPanel extends VisualActorViewPanel  {
	protected static final long serialVersionUID = 9212208191147321764L;
	
	protected VisualActorBehaviourView visualActorBehaviourView;

	protected int lastLayoutIndex;
	
	public static final AtomicInteger layoutIndex = new AtomicInteger(0);

	public VisualActorBehaviourViewPanel(InternalActorSystem system) {
		super(system);
		
		visualActorBehaviourView = new VisualActorBehaviourView(new AtomicReference<>(system)) {
			@Override
			public void removeVertex(Object source) {
				graph.getModel().remove(source);
			}

			@Override
			public Object addVertex(String name, String color) {
				return VisualActorBehaviourViewPanel.this.addVertex(name, color);
			}

			@Override
			public void addEdge(String value, Object source, Object target) {
				VisualActorBehaviourViewPanel.this.addEdge(value, source, target);
			}

			@Override
			public boolean updateValue(Object edge, String newValue, Object source, Object dest) {
				boolean result = false;
				
				if (((mxCell)edge).getSource()==source && ((mxCell)edge).getTarget()==dest) {
					((mxCell)edge).setValue(newValue);
					result = true;
				}
				
				return result;
			}

			@Override
			public Object[] getEdgesBetween(Object source, Object target) {
				return graph.getEdgesBetween(source, target);
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
				VisualActorBehaviourViewPanel.this.updateStructure();
			}
		};
		
		lastLayoutIndex = -1;
		
		add("Behaviour", paDesign);
	}
	
	public String analyzeBehaviour(Map<UUID, InternalActorCell> actorCells, Map<UUID, Map<UUID, Long>> deliveryRoutes, boolean showRootSystem, boolean colorize) {
		return visualActorBehaviourView.analyzeBehaviour(actorCells, deliveryRoutes, showRootSystem, colorize);
	}

	@Override
	public void updateStructure() {
		resetViewport();
		
		int currentLayoutIndex = layoutIndex.get();
		if (visualActorBehaviourView.isChanged() || lastLayoutIndex!=currentLayoutIndex) {
			mxGraphLayout layout = null;
			switch (currentLayoutIndex){
				case 0: {
					layout = new mxFastOrganicLayout(graph);
					// the higher, the more separated
					((mxFastOrganicLayout)layout).setForceConstant(60); 
					// true transforms the edges and makes them direct lines
					((mxFastOrganicLayout)layout).setDisableEdgeStyle(false); 	
					break;
				}
				case 1: {
					layout = new mxHierarchicalLayout(graph);
					((mxHierarchicalLayout)layout).setFineTuning(false);
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
