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
package io.actor4j.streams.core.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.mutable.MutableObject;

import io.actor4j.core.actors.ActorRef;
import io.actor4j.core.immutable.ImmutableList;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorGroup;
import io.actor4j.core.utils.ActorGroupList;
import io.actor4j.core.utils.Pair;
import io.actor4j.streams.core.ActorStream;

public class SortRecursiveStream<T extends Comparable<? super T>> extends ActorStream<T, T> {
	public SortRecursiveStream(final SortStreamType type, final int threshold) {
		this(null, type, threshold);
	}
	
	protected void swap(List<T> list, int i, int j) {
		T temp = list.get(i);
		list.set(i, list.get(j));
		list.set(j, temp);
	}
	
	@SuppressWarnings("unchecked")
	public SortRecursiveStream(final String name, final SortStreamType type, final int threshold) {
		super(name, 2/*binary tree, recursive decomposition*/);
		node.threshold = threshold;
		
		// Lomuto partition
		partition((list) -> {
			T pivot = list.getLast();
			int i=-1;
			for (int j=0; j<list.size()-1; j++) {
				if (type == SortStreamType.SORT_ASCENDING) {
					if (list.get(j).compareTo(pivot)<=0) {
						i++;
						swap(list, i, j);
					}
				}
				else {
					if (list.get(j).compareTo(pivot)>=0) {
						i++;
						swap(list, i, j);
					}
				}
			}
			swap(list, i+1, list.size()-1);
			
			return Pair.of(i+1, list);
		});

		flatMap(new Function<List<T>, List<T>>() {
			@Override
			public List<T> apply(List<T> list) {
				List<T> result = new ArrayList<>(list);
				
				if (type == SortStreamType.SORT_ASCENDING)
					Collections.sort(result);
				else
					Collections.sort(result, Collections.reverseOrder());
				
				return result;
			}
		});
		
		merge((map, pivot) -> {
			List<T> result = new ArrayList<>();
			
			MutableObject<Integer> index = new MutableObject<>();
			MutableObject<Boolean> first = new MutableObject<>(true);
			map.forEach((rank, list) -> {
				if (first.getValue()) {
					index.setValue(list.size());
					first.setValue(false);
				}
				result.addAll(list);
			});
			result.add(index.getValue(), (T)pivot);
			
			return result;
		});
	}
	
	public static <T> void scatter(List<T> list, Object pivotIndex, int tag, ActorRef actorRef, ActorGroup group) {
		actorRef.send(ActorMessage.create(new ImmutableList<T>(list.subList(0, (int)pivotIndex)), tag, actorRef.self(), ((ActorGroupList)group).get(0)));
		actorRef.send(ActorMessage.create(new ImmutableList<T>(list.subList((int)pivotIndex+1, list.size())), tag, actorRef.self(), ((ActorGroupList)group).get(1)));
	}
}
