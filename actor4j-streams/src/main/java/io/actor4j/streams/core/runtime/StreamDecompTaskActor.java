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
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.actor4j.core.actors.Actor;
import io.actor4j.core.actors.ActorDistributedGroupMember;
import io.actor4j.core.immutable.ImmutableList;
import io.actor4j.core.mutable.MutableObject;
import io.actor4j.core.utils.ActorGroup;
import io.reactivex.rxjava3.core.Observable;

public abstract class StreamDecompTaskActor<T, R> extends Actor implements ActorDistributedGroupMember {
	protected final ActorStreamDecompOperations<T, R> operations;
	protected final ActorGroup group;
	protected final ActorGroup hubGroup;
	protected final int dest_tag;
	
	protected final MutableObject<List<R>> result;
	
	public StreamDecompTaskActor(String name, ActorStreamDecompOperations<T, R> operations, ActorGroup group, ActorGroup hubGroup, int dest_tag) {
		super(name);
		
		this.operations = operations;
		this.group = group;
		this.hubGroup = hubGroup;
		this.dest_tag = dest_tag;
		
		result = new MutableObject<>();
	}

	@SuppressWarnings("unchecked")
	protected void executeOperations(ImmutableList<T> immutableList) {
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
	}

	@Override
	public UUID getDistributedGroupId() {
		return group.getId();
	}
}