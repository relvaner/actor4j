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

import io.actor4j.polyglot.pods.PolyglotContext;
import io.actor4j.polyglot.pods.PolyglotFunctionPod;

public class ExamplePolyglotFunctionPod_JAVA extends PolyglotFunctionPod {
	@Override
	public String domain() {
		return "ExamplePolyglotFunctionPod_JAVA";
	}

	@Override
	public String languageId() {
		return PolyglotContext.LANGUAGE_ID_JAVA;
	}

	@Override
	public CharSequence script() {
		return """
			import java.util.Map;
			import io.actor4j.polyglot.api.ActorPolyglotAPI;
			import io.actor4j.polyglot.api.ActorPolyglotMessage;
			
			public class Script {
				public static Object execute(ActorPolyglotAPI api, ActorPolyglotMessage message) {
					api.info("welcome");
					api.info(message.value());
				
					return Map.of("value", "Hello Test!", "tag", 42);
				}
			}
		""";
	}
}
