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
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.UUID;

import io.actor4j.analyzer.runtime.visual.Utils;
import io.actor4j.core.runtime.ActorExecutionUnitPoolHandler;
import io.actor4j.core.runtime.DefaultActorExecutionUnitPoolHandler;
import io.actor4j.core.runtime.InternalActorCell;
import io.actor4j.core.runtime.InternalActorExecutorService;
import io.actor4j.core.runtime.InternalActorSystem;

public abstract class VisualActorStructureView {
	protected final AtomicReference<InternalActorSystem> system;
	
	protected final Map<UUID, Boolean> activeCells;
	protected final Map<UUID, Object>  cells;
	
	protected Object defaultRoot;
	protected boolean changed;

	public VisualActorStructureView(AtomicReference<InternalActorSystem> system) {
		this.system = system;
		
		activeCells = new HashMap<>();
		cells = new HashMap<>();
	}
			
	public boolean isChanged() {
		return changed;
	}

	public void analyzeStructure(Map<UUID, InternalActorCell> actorCells, boolean showDefaultRoot, boolean showRootSystem, boolean colorize) {
		Iterator<Entry<UUID, Boolean>> iteratorActiveCells = activeCells.entrySet().iterator();
		while (iteratorActiveCells.hasNext())
			iteratorActiveCells.next().setValue(false);
		changed = false;
		
		graphUpdate(() -> {
        	if (showDefaultRoot && defaultRoot==null)
        		defaultRoot = addVertex("actor4j", "white");
        	
        	analyzeRootActor(actorCells, actorCells.get(system.get().USER_ID()), "yellow", showDefaultRoot, colorize);
        	if (showRootSystem)
        		analyzeRootActor(actorCells, actorCells.get(system.get().SYSTEM_ID()), "yellow", showDefaultRoot, colorize);
        	else
        		showOnlyRootActor(actorCells, actorCells.get(system.get().SYSTEM_ID()), "yellow", showDefaultRoot);
        	analyzeRootActor(actorCells, actorCells.get(system.get().UNKNOWN_ID()), "yellow", showDefaultRoot, colorize);
        	analyzeRootActor(actorCells, actorCells.get(system.get().PSEUDO_ID()), "yellow", showDefaultRoot, colorize);
        	
//        	Iterator<Entry<UUID, Boolean>> iteratorActiveCells_ = activeCells.entrySet().iterator();
        	while (iteratorActiveCells.hasNext()) {
        		Entry<UUID, Boolean> entry = iteratorActiveCells.next();
        		if (!entry.getValue()) {
        			//graph.removeCells(graph.getChildVertices(cells.get(entry.getKey())), true);
        			//graph.removeCells(new Object[] {cells.get(entry.getKey())}, true);
        			removeVertex(cells.get(entry.getKey()));
        			cells.remove(entry.getKey());
        			iteratorActiveCells.remove();
        			changed = true;
        		}		
        	}
		});
	}
	
	public void showOnlyRootActor(Map<UUID, InternalActorCell> actorCells, InternalActorCell root, String color, boolean showDefaultRoot) {
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
		}
	}
	
	public void analyzeRootActor(Map<UUID, InternalActorCell> actorCells, InternalActorCell root, String color, boolean showDefaultRoot, boolean colorize) {
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
	
	public void analyzeActor(Map<UUID, InternalActorCell> actorCells, InternalActorCell parent, Object parentVertex, boolean colorize) {
		Iterator<UUID> iterator = parent.getChildren().iterator();
		while (iterator.hasNext()) {
			InternalActorCell child = actorCells.get(iterator.next());
			if (child!=null) {
				if (activeCells.put(child.getId(), true)==null) {
					Object childVertex;
					
					String color = null;
					if (colorize) {
						ActorExecutionUnitPoolHandler<?> poolHandler = ((InternalActorExecutorService<?>)system.get().getExecutorService()).getExecutionUnitPool().getExecutionUnitPoolHandler();
        				if (poolHandler instanceof DefaultActorExecutionUnitPoolHandler<?> ph) {
	        				Long threadId = ph.getCellsMap().get(child.getId());
	        				if (threadId!=null)
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
					
					if (child.getActor().getName()!=null)
						childVertex = addVertex(child.getActor().getName(), color);
					else {
    					Optional<Entry<String, Queue<UUID>>> optional = system.get().getAliases().entrySet()
    						.stream().filter((entry) -> entry.getValue().contains(child.getId())).findFirst();
    					if (optional.isPresent())
    						childVertex = addVertex(optional.get().getKey(), color);
    					else
    						childVertex = addVertex(child.getId().toString(), color);
    				}	
				
					addEdge(null, parentVertex, childVertex);
					
					cells.put(child.getId(), childVertex);
					changed = true;
				}
				
				
				analyzeActor(actorCells, child, cells.get(child.getId()), colorize);
			}
		}
	}

	public abstract void removeVertex(Object source);
	public abstract Object addVertex(String name, String color);
	public abstract void addEdge(String value, Object source, Object target);
	
	public abstract void graphUpdate(Runnable run);
	public abstract void updateStructure();
}
