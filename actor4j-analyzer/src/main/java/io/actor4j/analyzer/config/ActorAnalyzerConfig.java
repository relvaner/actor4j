/*
 * Copyright (c) 2015-2021, David A. Bauer. All rights reserved.
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
package io.actor4j.analyzer.config;

import io.actor4j.core.config.ActorSystemConfig;

public class ActorAnalyzerConfig extends ActorSystemConfig {
	public static abstract class Builder<T extends ActorAnalyzerConfig> extends ActorSystemConfig.Builder<T> 	{
		public Builder() {
			super();
			name = "actor4j-analyzer";
			sleepMode();
		}
		
		public Builder(T config) {
			super(config);
			name = "actor4j-analyzer";
			sleepMode();
		}
	}

	public ActorAnalyzerConfig(Builder<?> builder) {
		super(builder);
	}
	
	public static ActorAnalyzerConfig create() {
		return new ActorAnalyzerConfig(builder());
	}
	
	public static Builder<?> builder() {
		return new Builder<ActorAnalyzerConfig>() {
			@Override
			public ActorAnalyzerConfig build() {
				return new ActorAnalyzerConfig(this);
			}
		};
	}
	
	public static Builder<?> builder(ActorAnalyzerConfig config) {
		return new Builder<ActorAnalyzerConfig>(config) {
			@Override
			public ActorAnalyzerConfig build() {
				return new ActorAnalyzerConfig(this);
			}
		};
	}
}
