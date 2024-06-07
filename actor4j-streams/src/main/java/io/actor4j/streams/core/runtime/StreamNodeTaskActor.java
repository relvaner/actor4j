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

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.mutable.MutableObject;

import io.actor4j.core.actors.Actor;
import io.actor4j.core.actors.ActorDistributedGroupMember;
import io.actor4j.core.immutable.ImmutableList;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorGroup;
import io.actor4j.core.utils.ActorGroupList;
import io.reactivex.rxjava3.core.Observable;

import static io.actor4j.core.utils.CommPattern.*;
import static io.actor4j.streams.core.runtime.ActorMessageTag.*;

public class StreamNodeTaskActor<T, R> extends Actor implements ActorDistributedGroupMember {
	protected final ActorStreamNodeOperations<T, R> operations;
	protected final BinaryOperator<List<R>> defaultReduceOp;
	protected final ActorGroupList group;
	protected final ActorGroup hubGroup;
	protected int dest_tag;
	
	protected MutableObject<List<R>> result;
	protected int level;
	
	public StreamNodeTaskActor(String name, ActorStreamNodeOperations<T, R> operations, ActorGroupList group, ActorGroup hubGroup, int dest_tag) {
		super(name);
		
		this.operations = operations;
		defaultReduceOp = new BinaryOperator<List<R>>() {
			@Override
			public List<R> apply(List<R> left, List<R> right) {
				List<R> result = new ArrayList<>(left.size()+right.size());
				result.addAll(left);
				result.addAll(right);		
				return result;
			}
		};
		this.group = group;
		this.hubGroup = hubGroup;
		this.dest_tag = dest_tag;
		
		result = new MutableObject<>();
		level = -1;
		
		stash = new PriorityQueue<ActorMessage<?>>(11, (m1, m2) -> { 
			return Integer.valueOf(m1.protocol()).compareTo(Integer.valueOf(m2.protocol())); 
		} );
	}

	@SuppressWarnings("unchecked")
	protected void treeReduction(ActorMessage<?> message) {
		int grank = group.indexOf(self());
		if (grank%(1<<(level+1))>0) { 
			int dest = grank-(1<<level);
			//System.out.printf("[level: %d] rank %d has sended a message (%s) to rank %d%n", level, group.indexOf(self()), result.getValue().toString(), dest);
			send(ActorMessage.create(new ImmutableList<R>(result.getValue()), REDUCE, self(), group.get(dest), null, String.valueOf(level+1), null));
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
				ImmutableList<T> immutableList = (ImmutableList<T>)message.value();
				
				if (operations.streamOp!=null) {
					Stream<R> stream = operations.streamOp.apply(immutableList.get().stream());
					if (stream!=null)
						result.setValue(stream.collect(Collectors.toList()));
					else
						result.setValue(new ArrayList<R>());
				}
				else if (operations.streamRxOp!=null) {
					Observable<R> observable = operations.streamRxOp.apply(Observable.fromIterable(immutableList.get()));
					if (observable!=null)
						observable.toList().subscribe(list -> result.setValue(list));
					else
						result.setValue(new ArrayList<R>());
					/*
					 * result.setValue(observable.toList().blockingGet());
					 */
				}
				else if (operations.flatMapOp!=null) {
					List<R> list = operations.flatMapOp.apply(immutableList.get());
					if (list!=null)
						result.setValue(list);
					else
						result.setValue(new ArrayList<R>());
				}
				else {
					List<R> list = new ArrayList<>(immutableList.get().size());
					for (T t : immutableList.get()) {
						if (operations.filterOp!=null)
							if (!operations.filterOp.test(t))
								continue;
						if (operations.mapOp!=null)
							list.add(operations.mapOp.apply(t));
						else
							list.add((R)t);
						if (operations.forEachOp!=null)
							operations.forEachOp.accept(t);	
					}
					result.setValue(list);
				}

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

	@Override
	public UUID getDistributedGroupId() {
		return group.getId();
	}
}