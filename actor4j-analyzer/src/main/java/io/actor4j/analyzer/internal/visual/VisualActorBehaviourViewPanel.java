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
package io.actor4j.analyzer.internal.visual;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.mxgraph.layout.mxFastOrganicLayout;
import com.mxgraph.layout.mxGraphLayout;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.model.mxCell;

import io.actor4j.core.internal.InternalActorCell;
import io.actor4j.core.internal.InternalActorSystem;

import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingConstants;

public class VisualActorBehaviourViewPanel extends VisualActorViewPanel  {
	protected static final long serialVersionUID = 9212208191147321764L;
	
	protected Map<UUID, Boolean> activeCells;
	protected Map<UUID, Object>  cells;
	
	protected boolean changed;
	protected int lastLayoutIndex;
	
	public static final AtomicInteger layoutIndex = new AtomicInteger(0);

	public VisualActorBehaviourViewPanel(InternalActorSystem system) {
		super(system);
		
		activeCells = new HashMap<>();
		cells = new HashMap<>();
		
		lastLayoutIndex = -1;
		
		add("Behaviour", paDesign);
	}
	
	public void analyzeBehaviour(Map<UUID, InternalActorCell> actorCells, Map<UUID, Map<UUID, Long>> deliveryRoutes, boolean showRootSystem, boolean colorize) {
		Iterator<Entry<UUID, Boolean>> iteratorActiveCells = activeCells.entrySet().iterator();
		while (iteratorActiveCells.hasNext())
			iteratorActiveCells.next().setValue(false);
		changed = false;
		
		String color = null;
		graph.getModel().beginUpdate();
	    try {
	    	Iterator<InternalActorCell> iterator = actorCells.values().iterator();
	        while (iterator.hasNext()) {
	        	InternalActorCell actorCell = iterator.next(); 
	        	
	        	if (!showRootSystem && actorCell.isRootInSystem())
	        		continue;
	        	
	        	if (activeCells.put(actorCell.getId(), true)==null) {
	        		if (actorCell.getId()==system.USER_ID() || actorCell.getId()==system.SYSTEM_ID() || actorCell.getId()==system.UNKNOWN_ID() || actorCell.getId()==system.PSEUDO_ID())
	        			color = ";fillColor=yellow";
	        		else {
	        			if (colorize) {
	        				Long threadId = system.getExecuterService().getActorThreadPool().getActorThreadPoolHandler().getCellsMap().get(actorCell.getId());
	        				if (threadId!=null)
	        					color = ";fillColor="+Utils.randomColorAsHex(
	        						system.getExecuterService().getActorThreadPool().getActorThreadPoolHandler().getThreadsList().indexOf(threadId), 
	        						system.getConfig().parallelism()*system.getConfig().parallelismFactor());
	        				else
	        					color = ";fillColor=#F0F0F0";
	        			}
	        			else
	        				color = ";fillColor=#00FF00";
	        		}
	        		
    				Object vertex;
    				if (actorCell.getActor().getName()!=null)
    					vertex = addVertex(actorCell.getActor().getName(), color);
    				else
    					vertex = addVertex(actorCell.getId().toString(), color);
    			
    				cells.put(actorCell.getId(), vertex);
    				changed = true;
    			}
	        }
	    	 
	        iteratorActiveCells = activeCells.entrySet().iterator();
     		while (iteratorActiveCells.hasNext()) {
     			Entry<UUID, Boolean> entry = iteratorActiveCells.next();
     			if (!entry.getValue()) {
     				graph.getModel().remove(cells.get(entry.getKey()));
     				cells.remove(entry.getKey());
     				iteratorActiveCells.remove();
     				changed = true;
     			}		
     		}
     			
     		Iterator<Entry<UUID, Object>> iteratorCells = cells.entrySet().iterator();
     		while (iteratorCells.hasNext()) {
     			Entry<UUID, Object> entry = iteratorCells.next();
     			
     			analyzeDeliveryRoutes(deliveryRoutes, entry);
     		}
     		
	    } finally {
			graph.getModel().endUpdate();
		}
	    graphComponent.refresh();
	}
	
	public void analyzeDeliveryRoutes(Map<UUID, Map<UUID, Long>> deliveryRoutes, Entry<UUID, Object> entrySource) {
		Map<UUID, Long> routes = deliveryRoutes.get(entrySource.getKey());
		
		if (routes!=null) {
			Object source = entrySource.getValue();
			Iterator<Entry<UUID, Long>> iterator = routes.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<UUID, Long> entry = iterator.next();
				Object dest = null;
				if ((dest=cells.get(entry.getKey()))!=null) {
					Object[] edges = null;
					if ((edges=graph.getEdgesBetween(source, dest))!=null && edges.length>0) {
						boolean found=false;
						for (Object edge : edges)
							if (((mxCell)edge).getSource()==source && ((mxCell)edge).getTarget()==dest) {
								((mxCell)edge).setValue(entry.getValue());
								found=true;
								break;
							}
						if (!found) {
							addEdge(entry.getValue().toString(), source, dest);
							changed = true;
						}
					}
					else {
						addEdge(entry.getValue().toString(), source, dest);
						changed = true;
					}
				}
			}
		}
		
	}

	@Override
	public void updateStructure() {
		resetViewport();
		
		int currentLayoutIndex = layoutIndex.get();
		if (changed || lastLayoutIndex!=currentLayoutIndex) {
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
