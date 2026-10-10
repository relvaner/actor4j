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
package io.actor4j.polyglot.pods;

import java.util.function.Function;

import org.graalvm.polyglot.Value;

import io.actor4j.core.actors.ActorRef;
import io.actor4j.core.json.JsonObject;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.pods.PodContext;
import io.actor4j.core.pods.functions.PodFunction;
import io.actor4j.core.utils.Reply;
import io.actor4j.polyglot.api.utils.ValueToJsonMapper;
import io.actor4j.polyglot.pods.runtime.PolyglotContextInternal;

public class PolyglotPodFunction extends PodFunction {
	protected final PolyglotContextInternal contextPolyglot;
	protected final CharSequence script;
	protected final Function<Value, Object> defaultMapper;
	
	public PolyglotPodFunction(ActorRef host, PodContext context, PolyglotContextInternal contextPolyglot, CharSequence script, Function<Value, Object> defaultMapper) {
		super(host, context);
		this.contextPolyglot = contextPolyglot;
		this.script = script;
		this.defaultMapper = defaultMapper;
	}
	
	public PolyglotPodFunction(ActorRef host, PodContext context, PolyglotContextInternal contextPolyglot, CharSequence script) {
		this(host, context, contextPolyglot, script, (v) -> ValueToJsonMapper.convertValue(v));
	}

	@Override
	public Reply handle(ActorMessage<?> message) {
		Reply result;
		
		try {
			Value resultValue = contextPolyglot.executeFunction(host, context, message, script);
			if (resultValue!=null && !resultValue.isNull()) {
				Object mappedObject = defaultMapper.apply(resultValue);
				
				int tag = 0;
				if (mappedObject!=null) {
					if (mappedObject instanceof JsonObject obj && obj.containsKey(PolyglotContext.TAG))
						tag = obj.getInteger(PolyglotContext.TAG);
				}
				
				result = Reply.of(mappedObject, tag);
			}
			else 	
				result = Reply.none();
		}
		catch (Exception e) {
			e.printStackTrace();
			
			result = Reply.of(JsonObject.create().put(PolyglotContext.ERROR, e.getMessage()), 0);
		}
		
		return result;
	}
}
