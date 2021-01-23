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
package io.actor4j.analyzer.visual;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.mxgraph.layout.mxFastOrganicLayout;
import com.mxgraph.layout.mxParallelEdgeLayout;

import io.actor4j.core.ActorCell;
import io.actor4j.core.ActorSystemImpl;

public class VisualActorStructureViewPanel extends VisualActorViewPanel {
	protected static final long serialVersionUID = -1192782222987329027L;
	
	protected Map<UUID, Boolean> activeCells;
	protected Map<UUID, Object>  cells;
	
	protected Object defaultRoot;
	protected boolean changed;

	public VisualActorStructureViewPanel(ActorSystemImpl system) {
		super(system);
		
		activeCells = new HashMap<>();
		cells = new HashMap<>();
		
		add("Structure", paDesign);
	}
			
	public void analyzeStructure(Map<UUID, ActorCell> actorCells, boolean showDefaultRoot, boolean colorize) {
		Iterator<Entry<UUID, Boolean>> iteratorActiveCells = activeCells.entrySet().iterator();
		while (iteratorActiveCells.hasNext())
			iteratorActiveCells.next().setValue(false);
		changed = false;
		
        graph.getModel().beginUpdate();
        try {
        	if (showDefaultRoot && defaultRoot==null)
        		defaultRoot = addVertex("actor4j", ";fillColor=white");
        	
        	analyzeRootActor(actorCells, actorCells.get(system.USER_ID),    ";fillColor=yellow", showDefaultRoot, colorize);
        	analyzeRootActor(actorCells, actorCells.get(system.SYSTEM_ID),  ";fillColor=yellow", showDefaultRoot, colorize);
        	analyzeRootActor(actorCells, actorCells.get(system.UNKNOWN_ID), ";fillColor=yellow", showDefaultRoot, colorize);
        	
        	iteratorActiveCells = activeCells.entrySet().iterator();
        	while (iteratorActiveCells.hasNext()) {
        		Entry<UUID, Boolean> entry = iteratorActiveCells.next();
        		if (!entry.getValue()) {
        			//graph.removeCells(graph.getChildVertices(cells.get(entry.getKey())), true);
        			//graph.removeCells(new Object[] {cells.get(entry.getKey())}, true);
        			graph.getModel().remove(cells.get(entry.getKey()));
        			cells.remove(entry.getKey());
        			iteratorActiveCells.remove();
        			changed = true;
        		}		
        	}
		} finally {
			graph.getModel().endUpdate();
		}
        graphComponent.refresh();
	}
	
	public void analyzeRootActor(Map<UUID, ActorCell> actorCells, ActorCell root, String color, boolean showDefaultRoot, boolean colorize) {
		if (root!=null) {
			if (activeCells.put(root.getId(), true)==null) {
				Object rootVertex;
				if (root.getActor().getName()!=null)
					rootVertex = addVertex(root.getActor().getName(), color);
				else
					rootVertex = addVertex(root.getId().toString(), color);
			
				if (showDefaultRoot)
					addEdge(null, defaultRoot, rootVertex);
				
				cells.put(root.getId(), rootVertex);
				changed = true;
			}
			
			analyzeActor(actorCells, root, cells.get(root.getId()), colorize);
		}
	}
	
	public void analyzeActor(Map<UUID, ActorCell> actorCells, ActorCell parent, Object parentVertex, boolean colorize) {
		Iterator<UUID> iterator = parent.getChildren().iterator();
		while (iterator.hasNext()) {
			ActorCell child = actorCells.get(iterator.next());
			if (child!=null) {
				if (activeCells.put(child.getId(), true)==null) {
					Object childVertex;
					
					String color = null;
					if (colorize) {
        				long threadId = system.getExecuterService().getActorThreadPool().getActorThreadPoolHandler().getCellsMap().get(child.getId());
        				color = ";fillColor="+Utils.randomColorAsHex(
        						system.getExecuterService().getActorThreadPool().getActorThreadPoolHandler().getThreadsList().indexOf(threadId), 
        						system.getParallelismMin()*system.getParallelismFactor());
        			}
        			else
        				color = ";fillColor=#00FF00";
					
					if (child.getActor().getName()!=null)
						childVertex = addVertex(child.getActor().getName(), color);
					else
						childVertex = addVertex(child.getId().toString(), color);
				
					addEdge(null, parentVertex, childVertex);
					
					cells.put(child.getId(), childVertex);
					changed = true;
				}
				
				
				analyzeActor(actorCells, child, cells.get(child.getId()), colorize);
			}
		}
	}

	@Override
	public void updateStructure() {
		resetViewport();
		 
		if (changed) {
			mxFastOrganicLayout layout = new mxFastOrganicLayout(graph);
			layout.setForceConstant(40); 			// the higher, the more separated
			layout.setDisableEdgeStyle( false); 	// true transforms the edges and makes them direct lines
			layout.execute(graph.getDefaultParent());
		
			//new mxCircleLayout(graph).execute(graph.getDefaultParent());
			new mxParallelEdgeLayout(graph).execute(graph.getDefaultParent());
		}
	    
	    fitViewport();
	}
}
