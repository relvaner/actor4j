/*
 * Copyright (c) 2015-2020, David A. Bauer. All rights reserved.
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
package io.actor4j.streams.core.runtime;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class ActorStreamDecompNode<T, R> {
	public final UUID id;
	public final String alias;
	public List<T> data;
	public final ActorStreamDecompOperations<T, R> operations;
	public int nTasks;
	public int threshold;
	public final Set<ActorStreamDecompNode<?, ?>> sucs; // Set<Node<R, ?>>
	public final Set<ActorStreamDecompNode<?, ?>> pres; // Set<Node<?, T>>
	public boolean isRoot;
	public final int recursiveDecomp;

	public CountDownLatch rootCountDownLatch;
	
	public ActorStreamDecompNode(String alias, int recursiveDecomp) {
		super();
		
		this.id = UUID.randomUUID();
		this.alias = alias;
		
		this.operations = new ActorStreamDecompOperations<>();
		
		this.sucs = new HashSet<>();
		this.pres = new HashSet<>();
		
		this.recursiveDecomp = recursiveDecomp;
	}
	
	public ActorStreamDecompNode(String alias) {
		this(alias, -1);
	}
	
	public ActorStreamDecompNode() {
		this(null, -1);
	}
}
