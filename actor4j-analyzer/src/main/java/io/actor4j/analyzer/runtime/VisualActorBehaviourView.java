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
package io.actor4j.analyzer.runtime;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import io.actor4j.analyzer.runtime.visual.Utils;
import io.actor4j.analyzer.runtime.visual.Utils.Triple;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.runtime.ActorExecutionUnitPoolHandler;
import io.actor4j.core.runtime.DefaultActorExecutionUnitPoolHandler;
import io.actor4j.core.runtime.InternalActorCell;
import io.actor4j.core.runtime.InternalActorExecutorService;
import io.actor4j.core.runtime.InternalActorSystem;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public abstract class VisualActorBehaviourView {
	protected final AtomicReference<InternalActorSystem> system;
	
	protected final Map<ActorId, Boolean> activeCells;
	protected final Map<ActorId, Object>  cells;
	
	protected boolean changed;

	public VisualActorBehaviourView(AtomicReference<InternalActorSystem> system) {
		this.system = system;
		
		activeCells = new HashMap<>();
		cells = new HashMap<>();
	}
	
	public boolean isChanged() {
		return changed;
	}
	
	public String analyzeBehaviour(Set<InternalActorCell> actorCells, Map<ActorId, Map<ActorId, Long>> deliveryRoutes, boolean showRootSystem, boolean colorize) {
		Iterator<Entry<ActorId, Boolean>> iteratorActiveCells = activeCells.entrySet().iterator();
		while (iteratorActiveCells.hasNext())
			iteratorActiveCells.next().setValue(false);
		changed = false;
		
		graphUpdate(() -> {
			String color = null;
	        for (InternalActorCell actorCell : actorCells) {
	        	if (!showRootSystem && actorCell.isRootInSystem())
	        		continue;
	        	
	        	if (activeCells.put(actorCell.getId(), true)==null) {
	        		if (actorCell.getId()==system.get().USER_ID() || actorCell.getId()==system.get().SYSTEM_ID() || actorCell.getId()==system.get().UNKNOWN_ID() || actorCell.getId()==system.get().PSEUDO_ID())
	        			color = "yellow";
	        		else {
	        			if (colorize) {
	        				ActorExecutionUnitPoolHandler<?> poolHandler = ((InternalActorExecutorService<?>)system.get().getExecutorService()).getExecutionUnitPool().getExecutionUnitPoolHandler();
	        				if (poolHandler instanceof DefaultActorExecutionUnitPoolHandler<?> ph) {
	        					Long threadId = actorCell.getThreadId();
		        				if (threadId>0)
		        					color = Utils.randomColorAsHex(
		        						ph.getExecutionUnitList().indexOf(threadId), 
		        						system.get().getConfig().parallelism()*system.get().getConfig().parallelismFactor());
		        				else
		        					color = "#F0F0F0"; 
	        				}
	        				else
	        					color = "#F0F0F0";
	        			}
	        			else
	        				color = "#00FF00";
	        		}
	        		
    				Object vertex;
    				if (actorCell.getActor().getName()!=null)
    					vertex = addVertex(actorCell.getActor().getName(), color);
    				else {
    					Optional<Entry<String, Queue<ActorId>>> optional = system.get().getAliases().entrySet()
    						.stream().filter((entry) -> entry.getValue().contains(actorCell.getId())).findFirst();
    					if (optional.isPresent())
    						vertex = addVertex(optional.get().getKey(), color);
    					else
    						vertex = addVertex(actorCell.getId().toString(), color);
    				}	
    			
    				cells.put(actorCell.getId(), vertex);
    				changed = true;
    			}
	        }
	    	 
	        Iterator<Entry<ActorId, Boolean>> iteratorActiveCells_ = activeCells.entrySet().iterator();
     		while (iteratorActiveCells_.hasNext()) {
     			Entry<ActorId, Boolean> entry = iteratorActiveCells_.next();
     			if (!entry.getValue()) {
     				removeVertex(cells.get(entry.getKey()));
     				cells.remove(entry.getKey());
     				iteratorActiveCells_.remove();
     				changed = true;
     			}		
     		}
     			
     		Iterator<Entry<ActorId, Object>> iteratorCells = cells.entrySet().iterator();
     		while (iteratorCells.hasNext()) {
     			Entry<ActorId, Object> entry = iteratorCells.next();
     			
     			analyzeDeliveryRoutes(deliveryRoutes, entry);
     		}
     		
	    });
	    
	    Set<ActorId> filter = new HashSet<>();
	    filter.add(system.get().SYSTEM_ID());
	    filter.addAll(((InternalActorCell)system.get().SYSTEM_ID()).getChildren());
	    Triple<Integer, Integer, Double> complexity = Utils.complexity(deliveryRoutes, filter);
	    DescriptiveStatistics statistics = Utils.weightStatistics(deliveryRoutes, filter);
	    double delta = statistics.getPercentile(50)/statistics.getMean();
	    return String.format(
	    	"View->Behaviour:  Active Actors: %d - Directed Edges: %d - Interactional Complexity: %.2f - "+
	    	"Mean: %.2f - Min: %.2f - Max: %.2f - SD: %.2f - Median: %.2f - Skewness: %.2f - Weighted Interactional Complexity: %.2f",
	    	complexity.a(), complexity.b(), complexity.c()*100, 
	    	statistics.getMean(), statistics.getMin(), statistics.getMax(), statistics.getStandardDeviation(), statistics.getPercentile(50), statistics.getSkewness(),
	    	complexity.c()*100*delta);
	}
	
	public void analyzeDeliveryRoutes(Map<ActorId, Map<ActorId, Long>> deliveryRoutes, Entry<ActorId, Object> entrySource) {
		Map<ActorId, Long> routes = deliveryRoutes.get(entrySource.getKey());
		
		if (routes!=null) {
			Object source = entrySource.getValue();
			Iterator<Entry<ActorId, Long>> iterator = routes.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<ActorId, Long> entry = iterator.next();
				Object dest = null;
				if ((dest=cells.get(entry.getKey()))!=null) {
					Object[] edges = null;
					if ((edges=getEdgesBetween(source, dest))!=null && edges.length>0) {
						boolean found=false;
						for (Object edge : edges)
							if (updateValue(edge, entry.getValue().toString(), source, dest)) {
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

	public abstract void removeVertex(Object source);
	public abstract Object addVertex(String name, String color);	
	public abstract void addEdge(String value, Object source, Object target);
	public abstract boolean updateValue(Object edge, String newValue, Object source, Object dest);
	public abstract Object[] getEdgesBetween(Object source, Object target);
	
	public abstract void graphUpdate(Runnable run);
	public abstract void updateStructure();
}
