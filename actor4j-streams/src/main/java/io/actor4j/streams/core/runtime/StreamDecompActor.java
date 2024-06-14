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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import io.actor4j.core.actors.Actor;
import io.actor4j.core.immutable.ImmutableList;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorGroup;
import io.actor4j.core.utils.ActorGroupList;
import io.actor4j.core.utils.ActorGroupSet;
import io.actor4j.core.utils.ConcurrentActorGroupQueue;
import io.actor4j.core.utils.Pair;
import io.actor4j.core.utils.Triple;
import io.actor4j.streams.core.exceptions.ActorStreamDataException;

import static io.actor4j.core.utils.CommPattern.*;
import static io.actor4j.streams.core.runtime.ActorMessageTag.*;

public class StreamDecompActor<T, R> extends Actor {
	protected final ActorStreamDecompNode<T, R> node;
	protected final ActorGroup hubGroup;
	protected int dest_tag;
	
	protected final boolean debugDataEnabled;
	// ThreadSafe
	protected final Map<UUID, List<?>> debugData;
	// ThreadSafe
	protected final Map<UUID, List<?>> result;
	// ThreadSafe
	protected final Map<String, UUID> aliases;
	
	protected final Set<UUID> waitForChildren;
	protected int waitForParents;
	
	protected static final Object lock = new Object();
	
	public StreamDecompActor(String name, ActorStreamDecompNode<T, R> node, Map<UUID, List<?>> result, Map<String, UUID> aliases, boolean debugDataEnabled, Map<UUID, List<?>> debugData) {
		super(name);
		
		this.node = node;
		
		this.debugDataEnabled = debugDataEnabled;
		this.debugData = debugData;
		this.result = result;
		this.aliases = aliases;
		
		waitForChildren = new HashSet<>(node.sucs.size());
		waitForParents = node.pres!=null ? node.pres.size() : 0;
		
		hubGroup = new ActorGroupSet();
	}
	
	public StreamDecompActor(String name, ActorStreamDecompNode<T, R> node, Map<UUID, List<?>> result, Map<String, UUID> aliases) {
		this(name, node, result, aliases, false, null);
	}
	
	@Override
	public void preStart() {
		setAlias("node-"+node.id.toString());
		
		if (aliases!=null && node.alias!=null)
			aliases.put(node.alias, node.id);
		if (debugDataEnabled && debugData!=null && node.data!=null)
			debugData.put(node.id, node.data);
		
		if (node.data==null)
			node.data = new LinkedList<>();

		if (node.sucs!=null)
			for (ActorStreamDecompNode<?, ?> suc : node.sucs) {
				UUID ref = null;
				if (suc.pres.size()>1) {
					// uses Double-Check-Idiom a la Bloch
					ref = getSystem().getActorFromAlias("node-"+suc.id.toString());
					if (ref==null) {
						synchronized(lock) {
							ref = getSystem().getActorFromAlias("node-"+suc.id.toString());
							if (ref==null) {
								suc.nTasks = node.nTasks; // ATTENTION
								ref = addChild(() ->
									new StreamDecompActor<>("node-"+suc.id.toString(), suc, result, aliases, debugDataEnabled, debugData)
								);
							}
						}
					}
				}
				else {
					suc.nTasks = node.nTasks; // ATTENTION
					ref = addChild(() ->
						new StreamDecompActor<>("node-"+suc.id.toString(), suc, result, aliases, debugDataEnabled, debugData)
					);
				}
				
				hubGroup.add(ref);
				waitForChildren.add(ref);
			}
	}

	protected int adjustSizeForMapReduce(int size, int arr_size, int threshold/*min_range*/) {
		if (threshold>0) {
			int max_size  = arr_size/threshold;
			if (max_size==0)
				max_size = 1;
			size = (size>max_size ? max_size : size);
		}
		
		if (arr_size<size)
			size = arr_size;
		
		return size;
	}
	
	protected void partitionForMapReduce() {
		ActorGroup group = new ConcurrentActorGroupQueue();
		checkData(node.data);
		node.nTasks = adjustSizeForMapReduce(node.nTasks, node.data.size(), node.threshold);
		for (int i=0; i<node.nTasks; i++) {
			final int i_ = i;
			UUID task = addChild(() -> 
				new StreamMapReduceTaskActor<>("task-"+UUID.randomUUID().toString()+"-rank-"+i_, node.operations, group, hubGroup, dest_tag)
			);
			group.add(task);
		}
		scatter(node.data, TASK, this, new ActorGroupSet(group));
	}
	
	protected void partitionForRecursiveDecomp() {
		ActorGroup group = new ConcurrentActorGroupQueue();
		ActorGroup scatter_group = new ActorGroupList();
		checkData(node.data);
		if (node.data.size()<node.threshold) {
			UUID task = addChild(() -> 
				new StreamRecursiveTaskActor<>("task-"+UUID.randomUUID().toString(), node.operations, node.recursiveDecomp, node.threshold, 
					group, hubGroup, dest_tag, 1, node.recursiveDecompScatter)
			);
			scatter_group.add(task);
			scatter(node.data, TASK, this, scatter_group);
		}
		else {
			final Triple<Object, Object, List<T>> triple;
			if (node.operations.partitionOp!=null) {
				triple = node.operations.partitionOp.apply(node.data);
				node.data = triple.c();
				checkData(node.data);
				
				node.nTasks = node.recursiveDecomp;
				// Guardian
				addChild(() -> new Actor() {
					protected Set<UUID> waitForChildren;
					protected Map<Long, List<R>> resultMap;
					protected Object criterion = triple.b();
					
					public void preStart() {
						waitForChildren = new HashSet<>();
						resultMap = new TreeMap<>();
						
						for (int i=0; i<node.nTasks; i++) {
							final int i_ = i;
							UUID task = addChild(() -> 
								new StreamRecursiveTaskActor<>("task-"+UUID.randomUUID().toString(), node.operations, node.recursiveDecomp, node.threshold, 
									group, null, dest_tag, i_+1, node.recursiveDecompScatter)
							);
							waitForChildren.add(task);
							scatter_group.add(task);
						}
						node.recursiveDecompScatter.scatter(node.data, triple.a()/*criterionIndex*/, TASK, this, scatter_group);
					}
					
					public void receive(ActorMessage<?> message) {
						waitForChildren.remove(message.source());
						
						if (message.value() instanceof Pair) {
							@SuppressWarnings("unchecked")
							Pair<Long, ImmutableList<R>> pair = (Pair<Long, ImmutableList<R>>)message.value();
							resultMap.put(pair.a(), pair.b().get());
						}
						
						if (waitForChildren.isEmpty()) {
							List<R> result = node.operations.mergeOp.apply(resultMap, criterion);
							broadcast(ActorMessage.create(new ImmutableList<R>(result), dest_tag, self(), null), this, hubGroup);
							stop();
						}
					}
				});
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void receive(ActorMessage<?> message) {
		if (message.tag()==DATA) {
			waitForParents--;
			
			if (message.value()!=null && message.value() instanceof ImmutableList)
				node.data.addAll(((ImmutableList<T>)message.value()).get());
			
			if (waitForParents<=0) {
				if (node.sucs==null || node.sucs.size()==0) {
					hubGroup.add(self());
					dest_tag = RESULT;
				}
				else
					dest_tag = DATA;
				
				if (debugDataEnabled && debugData!=null)
					debugData.put(node.id, node.data);
				
				if (node.recursiveDecomp>0)
					partitionForRecursiveDecomp();
				else
					partitionForMapReduce();
			}
		}
		else if (message.tag()==RESULT) {
			if (result!=null)
				result.put(node.id, ((ImmutableList<R>)message.value()).get());
			
			if (node.isRoot && node.rootCountDownLatch!=null)
				node.rootCountDownLatch.countDown();
			else {
				if (!node.pres.isEmpty()) // has more parents
					for (ActorStreamDecompNode<?, ?> pre : node.pres) 
						sendViaAlias(ActorMessage.create(null, SHUTDOWN, self(), null), "node-"+pre.id.toString());
				else
					send(ActorMessage.create(null, SHUTDOWN, self(), getParent()));
			}
		}
		else if (message.tag()==SHUTDOWN) {
			waitForChildren.remove(message.source());
			
			if (waitForChildren.isEmpty()) {
				if (node.isRoot && node.rootCountDownLatch!=null)
					node.rootCountDownLatch.countDown();
				else
					send(ActorMessage.create(null, SHUTDOWN, self(), getParent()));
			}
		}
	}
	
	protected void checkData(List<T> data) {
		if (data==null)
			throw new ActorStreamDataException();
	}
}
