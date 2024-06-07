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

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Node<T, R> {
	public UUID id;
	public String alias;
	public List<T> data;
	public final NodeOperations<T, R> operations;
	public int nTasks;
	public int min_range;
	public Set<Node<?, ?>> sucs; // Set<Node<R, ?>>
	public Set<Node<?, ?>> pres; // Set<Node<?, T>>
	public boolean isRoot;
	
	public Node(String alias) {
		super();
		
		this.alias = alias;
		operations = new NodeOperations<>();
	}
	
	public Node() {
		this(null);
	}
}
