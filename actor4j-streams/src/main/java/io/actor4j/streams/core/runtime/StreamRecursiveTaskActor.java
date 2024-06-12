/*
 * Copyright (c) 2015-2024, David A. Bauer. All rights reserved.
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

import static io.actor4j.core.utils.CommPattern.broadcast;
import static io.actor4j.streams.core.runtime.ActorMessageTag.TASK;
import static io.actor4j.streams.core.runtime.ActorMessageTag.RESULT;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import io.actor4j.core.immutable.ImmutableList;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorGroup;
import io.actor4j.core.utils.ActorGroupList;
import io.actor4j.core.utils.Pair;
import io.actor4j.core.utils.Triple;
import io.actor4j.streams.core.utils.RecursiveDecompScatter;

public class StreamRecursiveTaskActor<T, R> extends StreamDecompTaskActor<T, R> {
	protected final int recursiveDecomp;
	protected final int threshold;
	protected final long rank;
	
	protected final Set<UUID> waitForChildren;
	protected final Map<Long, List<R>> resultMap;
	
	protected Object criterion;
	
	protected final RecursiveDecompScatter<T> recursiveDecompScatter;
	
	public StreamRecursiveTaskActor(String name, ActorStreamDecompOperations<T, R> operations, int recursiveDecomp, int threshold, 
			ActorGroup group, ActorGroup hubGroup, int dest_tag, long rank, RecursiveDecompScatter<T> recursiveDecompScatter) {
		super(name, operations, group, hubGroup, dest_tag);
		
		this.recursiveDecomp = recursiveDecomp;
		this.threshold = threshold;
		this.rank = rank;
		this.recursiveDecompScatter = recursiveDecompScatter;
		
		waitForChildren = new HashSet<>();
		resultMap = new TreeMap<>();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void receive(ActorMessage<?> message) {
		if (message.tag()==TASK && message.value() instanceof ImmutableList) {
			ImmutableList<T> immutableList = (ImmutableList<T>)message.value();
			if (immutableList.get().size()<=threshold) {
				executeOperations(immutableList);
				if (hubGroup!=null)
					broadcast(ActorMessage.create(new ImmutableList<R>(result.getValue()), dest_tag, self(), null), this, hubGroup);
				else
					tell(Pair.of(rank, new ImmutableList<R>(result.getValue())), RESULT, getParent());
				stop();
			}
			else {
				ActorGroup scatter_group = new ActorGroupList();
				Triple<Object, Object, List<T>> triple = null;
				if (operations.partitionOp!=null) {
					triple = operations.partitionOp.apply(new ArrayList<>(immutableList.get()));
					criterion = triple.b();
				}
				for (int i=0; i<recursiveDecomp; i++) {
					final int i_ = i;
					UUID task = addChild(() -> 
						new StreamRecursiveTaskActor<>("task-"+UUID.randomUUID().toString(), operations, recursiveDecomp, threshold, 
								group, hubGroup, dest_tag, (rank<<recursiveDecomp/2)+i_+1, recursiveDecompScatter)
					);
					waitForChildren.add(task);
					scatter_group.add(task);
				}
				recursiveDecompScatter.scatter(triple.c(), triple.a()/*criterionIndex*/, TASK, this, scatter_group);
			}
		}
		else if (message.tag()==RESULT) {
			waitForChildren.remove(message.source());
			
			if (message.value() instanceof Pair) {
				Pair<Long, ImmutableList<R>> pair = (Pair<Long, ImmutableList<R>>)message.value();
				resultMap.put(pair.a(), pair.b().get());
			}
			
			if (waitForChildren.isEmpty()) {
				List<R> mergeOpResult = operations.mergeOp.apply(resultMap, criterion);
				tell(Pair.of(rank, new ImmutableList<R>(mergeOpResult)), RESULT, getParent());
				stop();
			}
		}
	}
}
