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
package io.actor4j.polyglot.pods.feature;

import io.actor4j.core.messages.ActorMessage;
import io.actor4j.polyglot.pods.PolyglotContext;
import io.actor4j.polyglot.pods.PolyglotFunctionPod;

public class ExamplePolyglotFunctionPod_JS2 extends PolyglotFunctionPod {
	@Override
	public String domain() {
		return "ExamplePolyglotFunctionPod_JS2";
	}

	@Override
	public String languageId() {
		return PolyglotContext.LANGUAGE_ID_JS;
	}

	@Override
	public CharSequence script() {
		return """
			function execute(api, message) {
				api.info("welcome");
				api.info(message.value());
				
				api.send({value: "Hello Test2!", tag: 423}, 423, null, message.value());
					
				return {value: "Hello Test!", tag: 42};
			}
		""";
	}
	
	@SuppressWarnings("unchecked")
	public ActorMessage<?> filter(ActorMessage<?> message) {
		return ((ActorMessage<String>)message).shallowCopy(message.valueAsId().globalId().toString());
	}
}
