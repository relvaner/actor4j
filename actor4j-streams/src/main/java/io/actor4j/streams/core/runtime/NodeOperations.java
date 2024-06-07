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
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import io.reactivex.rxjava3.core.Observable;

public class NodeOperations<T, R> {
	/* lazy  */
	public Predicate<T> filterOp;
	public Function<T, R> mapOp;
	public Consumer<T> forEachOp;
	/* eager */
	public Function<List<T>, List<R>> flatMapOp;
	public BinaryOperator<List<R>> reduceOp;
	
	public Function<Stream<T>, Stream<R>> streamOp; /* Java Streams */
	public Function<Observable<T>, Observable<R>> streamRxOp; /* RxJava */
}
