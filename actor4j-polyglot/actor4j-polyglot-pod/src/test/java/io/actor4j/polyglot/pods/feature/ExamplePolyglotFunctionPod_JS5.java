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

import io.actor4j.core.utils.CacheAsMap;
import io.actor4j.polyglot.pods.PolyglotContext;
import io.actor4j.polyglot.pods.PolyglotFunctionPod;
import io.actor4j.polyglot.state.PolyglotStateStore;

public class ExamplePolyglotFunctionPod_JS5 extends PolyglotFunctionPod {
	@Override
	public String domain() {
		return "ExamplePolyglotFunctionPod_JS5";
	}

	@Override
	public String languageId() {
		return PolyglotContext.LANGUAGE_ID_JS;
	}
	
	@Override
	public PolyglotStateStore createStateStore() {
		return new PolyglotStateStore(new CacheAsMap<>());
	}
	
	public void preStart(PolyglotContext contextPolyglot) {
		contextPolyglot.stateStore().put("result", 41);
	}

	@Override
	public CharSequence script() {
		return """
			function execute(api, message, state) {
				api.info("welcome");
				api.info(message.value());
				
				api.info(state.get("result"));
				state.put("result", state.get("result")+1);
				
				return {value: "Hello Test!", tag: state.get("result")};
			}
		""";
	}
}
