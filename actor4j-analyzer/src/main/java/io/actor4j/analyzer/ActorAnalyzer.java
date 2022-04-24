/*
 * Copyright (c) 2015-2017, David A. Bauer. All rights reserved.
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
package io.actor4j.analyzer;

import io.actor4j.analyzer.config.ActorAnalyzerConfig;
import io.actor4j.analyzer.runtime.ActorAnalyzerThread;
import io.actor4j.analyzer.runtime.AnalyzerActorSystemImpl;
import io.actor4j.core.ActorSystem;
import io.actor4j.core.config.ActorSystemConfig;

public interface ActorAnalyzer extends ActorSystem {
	public static ActorAnalyzer create(ActorAnalyzerThread analyzerThread) {
		return create(analyzerThread, null);
	}
	
	public static ActorAnalyzer create(ActorAnalyzerThread analyzerThread, ActorAnalyzerConfig config) {
		return new AnalyzerActorSystemImpl(analyzerThread, config);
	}
	
	@Deprecated
	@Override
	public default boolean setConfig(ActorSystemConfig config) {
		return false;
	}
	
	public boolean setConfig(ActorAnalyzerConfig config);
}
