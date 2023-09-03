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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.brunomnsilva.smartgraph.graph.Vertex;

import java.util.UUID;

import io.actor4j.analyzer.runtime.VisualActorStructureView;
import io.actor4j.core.runtime.InternalActorCell;
import io.actor4j.core.runtime.InternalActorSystem;

public class FXVisualActorStructureViewTab extends FXVisualActorViewTab {
	protected static final long serialVersionUID = -1192782222987329027L;
	
	protected VisualActorStructureView visualActorStructureView;

	protected int lastLayoutIndex;
	
	public static final AtomicInteger layoutIndex = new AtomicInteger(0);

	public FXVisualActorStructureViewTab(String text, AtomicReference<InternalActorSystem> system) {
		super(text, system);
		
		visualActorStructureView = new VisualActorStructureView(system) {
			@Override
			public void removeVertex(Object source) {
				graph.removeVertex((Vertex)source);
			}

			@Override
			public Object addVertex(String name, String color) {
				Object result = null;
				
				try {
					System.out.println(name);
					result = graph.insertVertex(new VertexElement(name));
				}
				catch(Exception e) {
					e.printStackTrace();
				}

				return result;
			}

			@Override
			public void addEdge(String value, Object source, Object target) {
				graph.insertEdge((Vertex)source, (Vertex)target, new EdgeElement(value!=null ? value : ""));
//				try {
//					graph.insertEdge((Vertex)source, (Vertex)target, new EdgeElement(value));
//				}
//				catch(Exception e) {
//					e.printStackTrace();
//				}
			}

			@Override
			public void graphUpdate(Runnable runnable) {
				runnable.run();
				
				graphView.update();
			}

			@Override
			public void updateStructure() {
				// empty
			}
		};
		
		lastLayoutIndex = -1;
	}
			
	public void analyzeStructure(Map<UUID, InternalActorCell> actorCells, boolean showDefaultRoot, boolean showRootSystem, boolean colorize) {
		visualActorStructureView.analyzeStructure(actorCells, showDefaultRoot, showRootSystem, colorize);
	}
}
