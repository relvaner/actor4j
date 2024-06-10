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
package io.actor4j.streams.core.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import io.actor4j.streams.core.ActorStream;

public class SortStream<T extends Comparable<? super T>> extends ActorStream<T, T> {
	public SortStream(final SortStreamType type) {
		this(null, type);
	}
	
	public SortStream(String name, final SortStreamType type) {
		super(name, false);

		flatMap(new Function<List<T>, List<T>>() {
			@Override
			public List<T> apply(List<T> list) {
				List<T> result = new ArrayList<>(list);
				
				if (type == SortStreamType.SORT_ASCENDING)
					Collections.sort(result);
				else
					Collections.sort(result, Collections.reverseOrder());
				
				return result;
			}});
						
		reduce(new BinaryOperator<List<T>>() {
			@Override
			public List<T> apply(List<T> left, List<T> right) {
				List<T> result = new ArrayList<>(left.size()+right.size());
						
				if (left.size()>0 && right.size()>0)
					if (type == SortStreamType.SORT_ASCENDING) {
						if (left.get(left.size()-1).compareTo(right.get(0)) < 0) {
							result.addAll(left);
							result.addAll(right);
									
							return result;
						}
					}
					else if (left.get(left.size()-1).compareTo(right.get(0)) > 0) {
						result.addAll(left);
						result.addAll(right);
									
						return result;
					}
										
				int leftPos = 0, rightPos = 0;
				for (int i=0; i<left.size()+right.size(); i++) {
					if (type == SortStreamType.SORT_ASCENDING) {
						if ( (leftPos<left.size()) && (rightPos==right.size() || left.get(leftPos).compareTo(right.get(rightPos))<0) ) {
							result.add(left.get(leftPos));
							leftPos++;
						}
						else {
							result.add(right.get(rightPos));
							rightPos++;
						}
					}
					else {
						if ( (leftPos<left.size()) && (rightPos==right.size() || left.get(leftPos).compareTo(right.get(rightPos))>0) ) {
							result.add(left.get(leftPos));
							leftPos++;
						}
						else {
							result.add(right.get(rightPos));
							rightPos++;
						}
					}	
				}
							
				return result;
			}});
	}
}
