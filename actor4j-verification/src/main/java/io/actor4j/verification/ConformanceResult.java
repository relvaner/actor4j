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
package io.actor4j.verification;

import java.util.List;

// result of the conformance check: messages observed at runtime that are not part of the models
public class ConformanceResult {
	protected final int observed;
	protected final List<String> violations;

	public ConformanceResult(int observed, List<String> violations) {
		super();
		this.observed = observed;
		this.violations = violations;
	}

	// number of checked messages
	public int observed() {
		return observed;
	}

	public List<String> violations() {
		return violations;
	}

	public boolean conforms() {
		return violations.isEmpty();
	}

	@Override
	public String toString() {
		return conforms() ? observed+" messages, conform to the models" : observed+" messages, violations: "+violations;
	}
}
