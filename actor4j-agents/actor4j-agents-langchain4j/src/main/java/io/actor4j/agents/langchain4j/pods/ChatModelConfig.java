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
package io.actor4j.agents.langchain4j.pods;

public class ChatModelConfig {
	private final String baseUrl;
	private final String apiKey;
	private final String modelName;
	private final double temperature;

	private ChatModelConfig(Builder builder) {
		this.baseUrl = builder.baseUrl;
		this.apiKey = builder.apiKey;
		this.modelName = builder.modelName;
		this.temperature = builder.temperature;
	}

	public static Builder create() {
		return new Builder();
	}

	public String baseUrl() {
		return baseUrl;
	}

	public String apiKey() {
		return apiKey;
	}

	public String modelName() {
		return modelName;
	}

	public double temperature() {
		return temperature;
	}

	@Override
	public String toString() {
		return "ChatModelConfig{" + "baseUrl='" + baseUrl + '\'' + ", apiKey='***'" + ", modelName='" + modelName + '\''
				+ ", temperature=" + temperature + '}';
	}

	public static class Builder {
		private String baseUrl = "http://localhost:9090/v1";
		private String apiKey = "dummy";
		private String modelName = "test";
		private double temperature = 0.2;

		public Builder baseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
			return this;
		}

		public Builder apiKey(String apiKey) {
			this.apiKey = apiKey;
			return this;
		}

		public Builder modelName(String modelName) {
			this.modelName = modelName;
			return this;
		}

		public Builder temperature(double temperature) {
			this.temperature = temperature;
			return this;
		}

		public ChatModelConfig build() {
			return new ChatModelConfig(this);
		}
	}
}
