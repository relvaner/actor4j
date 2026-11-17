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

import io.actor4j.core.pods.utils.PodStatus;
import io.actor4j.polyglot.pods.PolyglotContext;
import io.actor4j.polyglot.pods.PolyglotFunctionPod;

// "fail" reports a failure, "throw" throws in the script
public class ExamplePolyglotFunctionPod_JS7 extends PolyglotFunctionPod {
	@Override
	public String domain() {
		return "ExamplePolyglotFunctionPod_JS7";
	}

	@Override
	public String languageId() {
		return PolyglotContext.LANGUAGE_ID_JS;
	}

	@Override
	public CharSequence script() {
		return """
			function execute(api, message) {
				if (message.value()=="fail")
					return {"error": "Invalid request", "tag": %d};
				else if (message.value()=="throw")
					throw new Error("Script failed");

				return {"value": "OK", "tag": 42};
			}
		""".formatted(PodStatus.BAD_REQUEST);
	}
}
