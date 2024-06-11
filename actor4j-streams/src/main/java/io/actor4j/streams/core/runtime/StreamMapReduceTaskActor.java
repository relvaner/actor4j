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

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.BinaryOperator;

import io.actor4j.core.immutable.ImmutableList;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorGroup;
import io.actor4j.core.utils.ActorGroupList;

import static io.actor4j.core.utils.CommPattern.*;
import static io.actor4j.streams.core.runtime.ActorMessageTag.*;

public class StreamMapReduceTaskActor<T, R> extends StreamDecompTaskActor<T, R> {
	protected final BinaryOperator<List<R>> defaultReduceOp;
	protected int level;
	
	protected ActorGroupList groupList;
	
	public StreamMapReduceTaskActor(String name, ActorStreamDecompOperations<T, R> operations, ActorGroup group, ActorGroup hubGroup, int dest_tag) {
		super(name, operations, group, hubGroup, dest_tag);
		
		defaultReduceOp = new BinaryOperator<List<R>>() {
			@Override
			public List<R> apply(List<R> left, List<R> right) {
				List<R> result = new ArrayList<>(left.size()+right.size());
				result.addAll(left);
				result.addAll(right);		
				return result;
			}
		};

		level = -1;
		
		stash = new PriorityQueue<ActorMessage<?>>(11, (m1, m2) -> { 
			return Integer.valueOf(m1.protocol()).compareTo(Integer.valueOf(m2.protocol())); 
		} );
	}

	@SuppressWarnings("unchecked")
	protected void treeReduction(ActorMessage<?> message) {
		int grank = groupList.indexOf(self());
		if (grank%(1<<(level+1))>0) { 
			int dest = grank-(1<<level);
			//System.out.printf("[level: %d] rank %d has sended a message (%s) to rank %d%n", level, group.indexOf(self()), result.getValue().toString(), dest);
			send(ActorMessage.create(new ImmutableList<R>(result.getValue()), REDUCE, self(), groupList.get(dest), null, String.valueOf(level+1), null));
			stop();
		}
		else if (message.tag()==REDUCE && message.value()!=null && message.value() instanceof ImmutableList){
			List<R> buf = ((ImmutableList<R>)message.value()).get();
			//System.out.printf("[level: %d] rank %d has received a message (%s) from rank %d%n", level, group.indexOf(self()), buf.toString(), group.indexOf(message.source));
			if (operations.reduceOp!=null)
				result.setValue(operations.reduceOp.apply(result.getValue(), buf));
			else
				result.setValue(    defaultReduceOp.apply(result.getValue(), buf));
			
			level++;
			treeReduction(message.shallowCopy(TASK));
		}
		else {
			int source = grank+(1<<level);
			if (source>group.size()-1)
				if (grank==0) {
					broadcast(ActorMessage.create(new ImmutableList<R>(result.getValue()), dest_tag, self(), null), this, hubGroup);
					stop();
					return;
				} else {
					level++;
					treeReduction(message);
				}
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void receive(ActorMessage<?> message) {
		if (level<0) {
			if (message.tag()==TASK && message.value()!=null && message.value() instanceof ImmutableList) {
				groupList = new ActorGroupList(group); // Transform to LinkedList for treeReduction()
				executeOperations((ImmutableList<T>)message.value());
				
				level = 0;
				//System.out.printf("[level: %d] rank %d has got a message (%s) from manager %n", level, group.indexOf(self()), result.getValue().toString());
				
				treeReduction(message);
				dissolveStash();
			} 
			else if (message.tag()==REDUCE) {
				stash.offer(message);
			}
		}
		else if (message.tag()==REDUCE) {
			stash.offer(message);
			dissolveStash();
		}
	}
	
	protected void dissolveStash() {
		ActorMessage<?> message = null;
		while ((message = stash.peek())!=null) {
			if (Integer.valueOf(message.protocol())==level+1)
				treeReduction(stash.poll());
			else
				break;
		}
	}
}