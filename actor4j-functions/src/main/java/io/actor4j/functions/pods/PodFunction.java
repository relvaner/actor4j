/*
 * Copyright (c) 2015-2026, David A. Bauer. All rights reserved.
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
package io.actor4j.functions.pods;

import java.util.function.Function;

import io.actor4j.core.messages.ActorMessage;

public abstract class PodFunction {
	protected final FunctionPodContext context;
	protected final Function<Object, Object> defaultMapper;
	
	public static record Reply(Object value, int tag) {
		private static final Reply NONE = new Reply(null, -1);
		
		public static Reply of(Object value, int tag) {
			return new Reply(value, tag);
		}
		
		public static Reply none() {
			return NONE;
		}
		
		public boolean isNone() {
	        return tag < 0;
	    }
	}
	
	public PodFunction(FunctionPodContext context, Function<Object, Object> defaultMapper) {
		super();
		this.context = context;
		this.defaultMapper = defaultMapper;
	}
	
	public PodFunction(FunctionPodContext context) {
		this(context, (v) -> v);
	}

	public abstract Reply handle(ActorMessage<?> message);
}
