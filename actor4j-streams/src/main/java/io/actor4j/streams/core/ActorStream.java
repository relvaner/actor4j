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
package io.actor4j.streams.core;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import io.actor4j.streams.core.runtime.ActorStreamDecompNode;
import io.reactivex.rxjava3.core.Observable;

public class ActorStream<T, R> {
	protected final ActorStreamDecompNode<T, R> node;
	
	protected /*quasi final*/ Map<UUID, List<?>> data;   // initially set over ActorStreamManager
	protected /*quasi final*/ Map<UUID, List<?>> result; // initially set over ActorStreamManager
	protected /*quasi final*/ Map<String, UUID> aliases; // initially set over ActorStreamManager
	
	protected final ActorStreamOperations<T, R> processOperations;
	
	public ActorStream() {
		this(null, false);
	}
	
	public ActorStream(String alias) {
		this(alias, false);
	}
	
	public ActorStream(String alias, boolean recursiveDecomp) {
		super();
		
		node = new ActorStreamDecompNode<>(alias, recursiveDecomp);
		
		processOperations = new ActorStreamOperations<>(this);
	}
	
	public void configureAsRoot(ActorStreamManager manager, CountDownLatch countDownLatch) {
		data    = manager.data;
		result  = manager.result;
		aliases = manager.aliases;
		
		node.nTasks = manager.nTasks;
		node.rootCountDownLatch = countDownLatch;
		node.isRoot = true;
	}
	
	public ActorStream(Function<List<T>, List<R>> flatMapOp, BinaryOperator<List<R>> reduceOp) {
		this();
		
		node.operations.flatMapOp = flatMapOp;
		node.operations.reduceOp = reduceOp;
	}
	
	public UUID getId() {
		return node.id;
	}
	
	public ActorStreamOperations<T, R> data(List<T> data, int min_range) {
		return processOperations.data(data, min_range);
	}
	
	public ActorStreamOperations<T, R> data(List<T> data) {
		return processOperations.data(data);
	}
	
	public ActorStreamOperations<T, R> filter(Predicate<T> filterOp) {
		return processOperations.filter(filterOp);
	}
	
	public ActorStreamOperations<T, R> map(Function<T, R> mapOp) {
		return processOperations.map(mapOp);
	}
	
	public ActorStreamOperations<T, R> forEach(Consumer<T> forEachOp) {
		return processOperations.forEach(forEachOp);
	}
	
	public ActorStreamOperations<T, R> flatMap(Function<List<T>, List<R>> flatMapOp) {
		return processOperations.flatMap(flatMapOp);
	}
	
	public ActorStreamOperations<T, R> stream(Function<Stream<T>, Stream<R>> streamOp) {
		return processOperations.stream(streamOp);
	}
	
	public ActorStreamOperations<T, R> streamRx(Function<Observable<T>, Observable<R>> streamRxOp) {
		return processOperations.streamRx(streamRxOp);
	}
	
	public ActorStreamOperations<T, R> reduce(BinaryOperator<List<R>> reduceOp) {
		return processOperations.reduce(reduceOp);
	}	
	
	public ActorStreamOperations<?, ?> sortedASC() {
		return processOperations.sortedASC();
	}
	
	public ActorStreamOperations<?, ?> sortedDESC() {
		return processOperations.sortedDESC();
	}
			
	public <S> ActorStream<R, S> sequence(ActorStream<R, S> process) {
		node.sucs.add(process.node);
		process.data = data;
		process.result = result;
		
		return process;
	}
	
	public ActorStream<?, ?> sequence(List<ActorStream<?, ?>> processes) {
		ActorStream<?, ?> parent = this;
		if (processes!=null) {
			for (ActorStream<?, ?> p : processes) {
				parent.node.sucs.add(p.node);
				p.node.pres.add(parent.node);
				parent = p;
				p.data = data;
				p.result = result;
			}
		}
			
		return parent;
	}
	
	public ActorStream<?, ?> sequence(ActorStream<?, ?>... processes) {
		return sequence(Arrays.asList(processes));
	}
	
	public List<ActorStream<R, ?>> parallel(List<ActorStream<R, ?>> processes) {
		if (processes!=null)
			for (ActorStream<R, ?> p : processes)
				sequence(p);
		
		return processes;
	}
	
	@SuppressWarnings("unchecked")
	public List<ActorStream<R, ?>> parallel(ActorStream<R, ?>... processes) {
		return parallel(Arrays.asList(processes));
	}
	
	public ActorStream<T, R> merge(List<ActorStream<?, ?>> processes) {
		if (processes!=null) {
			if (processes.size()>0) {
				data = processes.get(0).data;
				result = processes.get(0).result;
			}
			for (ActorStream<?, ?> p : processes) {
				node.pres.add(p.node);
				p.node.sucs.add(node);
			}
		}
		
		return this;
	}
	
	public ActorStream<T, R> merge(ActorStream<?, ?>... processes) {
		return merge(Arrays.asList(processes));
	}
	
	public List<?> getData() {
		return node.data;
	}
	
	public List<?> getResult() {
		System.out.println(result); // TODO: BUG
		return result.get(node.id);
	}
}
