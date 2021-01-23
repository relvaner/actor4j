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
import java.util.UUID;

import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.layout.mxCompactTreeLayout;
import com.mxgraph.layout.mxFastOrganicLayout;
import com.mxgraph.layout.mxParallelEdgeLayout;
import com.mxgraph.model.mxCell;

import io.actor4j.core.ActorCell;
import io.actor4j.core.ActorSystemImpl;

import java.util.Map.Entry;

public class VisualActorBehaviourViewPanel extends VisualActorViewPanel  {
	protected static final long serialVersionUID = 9212208191147321764L;
	
	protected Map<UUID, Boolean> activeCells;
	protected Map<UUID, Object>  cells;
	
	protected boolean changed;

	public VisualActorBehaviourViewPanel(ActorSystemImpl system) {
		super(system);
		
		activeCells = new HashMap<>();
		cells = new HashMap<>();
		
		add("Behaviour", paDesign);
	}
	
	public void analyzeBehaviour(Map<UUID, ActorCell> actorCells, Map<UUID, Map<UUID, Long>> deliveryRoutes, boolean colorize) {
		Iterator<Entry<UUID, Boolean>> iteratorActiveCells = activeCells.entrySet().iterator();
		while (iteratorActiveCells.hasNext())
			iteratorActiveCells.next().setValue(false);
		changed = false;
		
		String color = null;
		graph.getModel().beginUpdate();
	    try {
	    	Iterator<ActorCell> iterator = actorCells.values().iterator();
	        while (iterator.hasNext()) {
	        	ActorCell actorCell = iterator.next(); 
	        	if (activeCells.put(actorCell.getId(), true)==null) {
	        		if (actorCell.getId()==system.USER_ID || actorCell.getId()==system.SYSTEM_ID || actorCell.getId()==system.UNKNOWN_ID)
	        			color = ";fillColor=yellow";
	        		else {
	        			if (colorize) {
	        				long threadId = system.getExecuterService().getActorThreadPool().getActorThreadPoolHandler().getCellsMap().get(actorCell.getId());
	        				color = ";fillColor="+Utils.randomColorAsHex(
	        						system.getExecuterService().getActorThreadPool().getActorThreadPoolHandler().getThreadsList().indexOf(threadId), 
	        						system.getParallelismMin()*system.getParallelismFactor());
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
		
		if (changed) {
			mxFastOrganicLayout layout = new mxFastOrganicLayout(graph);
			layout.setForceConstant(60); 			// the higher, the more separated
			layout.setDisableEdgeStyle( false); 	// true transforms the edges and makes them direct lines
			layout.execute(graph.getDefaultParent());

			//new mxCompactTreeLayout(graph).execute(graph.getDefaultParent());
			//new mxCircleLayout(graph).execute(graph.getDefaultParent());
			new mxParallelEdgeLayout(graph).execute(graph.getDefaultParent());
		}
		
		fitViewport();
	}
}
