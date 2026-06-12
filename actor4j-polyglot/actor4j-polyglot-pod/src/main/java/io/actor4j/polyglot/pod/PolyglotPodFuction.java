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
package io.actor4j.polyglot.pod;

import java.util.function.Function;

import org.graalvm.polyglot.Value;

import io.actor4j.core.actors.ActorRef;
import io.actor4j.core.json.JsonObject;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.pods.PodContext;
import io.actor4j.core.pods.functions.PodFunction;
import io.actor4j.core.utils.Pair;
import io.actor4j.polyglot.api.ValueMapper;

public class PolyglotPodFuction extends PodFunction {
	protected final PolyglotContext contextPolyglot;
	protected final CharSequence script;
	protected final Function<Value, Object> defaultMapper;
	
	public PolyglotPodFuction(ActorRef host, PodContext context, PolyglotContext contextPolyglot, CharSequence script, Function<Value, Object> defaultMapper) {
		super(host, context);
		this.contextPolyglot = contextPolyglot;
		this.script = script;
		this.defaultMapper = defaultMapper;
	}
	
	public PolyglotPodFuction(ActorRef host, PodContext context, PolyglotContext contextPolyglot, CharSequence script) {
		this(host, context, contextPolyglot, script, (v) -> ValueMapper.convertValue(v));
	}

	@Override
	public Pair<Object, Integer> handle(ActorMessage<?> message) {
		Pair<Object, Integer> result;
		
		try {
			Value resultValue = contextPolyglot.executeFunction(host, context, message, script);
			if (resultValue!=null && !resultValue.isNull()) {
				Object mappedObject = defaultMapper.apply(resultValue);
				
				int tag = 0;
				if (mappedObject!=null) {
					if (mappedObject instanceof JsonObject obj && obj.containsKey(PolyglotContext.TAG))
						tag = obj.getInteger(PolyglotContext.TAG);
				}
				
				result = Pair.of(mappedObject, tag);
			}
			else 	
				result = Pair.of(null, 0);
		}
		catch (Exception e) {
			e.printStackTrace();
			
			result = Pair.of(JsonObject.create().put(PolyglotContext.ERROR, e.getMessage()), 0);
		}
		
		return result;
	}
}
