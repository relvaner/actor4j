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

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * Configuration of a chat model, built in code or bound by Jackson, also as part of a larger
 * configuration (e.g. a field {@code ChatModelConfig model} in a record):
 *
 * <pre>
 * {
 *   "baseUrl": "http://localhost:9090/v1",
 *   "modelName": "test",
 *   "temperature": 0.2,
 *   "timeout": "PT10M",
 *   "customParameters": { "chat_template_kwargs": { "enable_thinking": false } }
 * }
 * </pre>
 *
 * Values not given keep the defaults of the builder. Unknown keys are ignored.
 */
@JsonDeserialize(builder = ChatModelConfig.Builder.class)
public class ChatModelConfig {
	private final String baseUrl;
	private final String apiKey;
	private final String modelName;
	private final double temperature;

	private final Duration timeout;
	private final int maxTokens;
	private final int maxRetries;
	private final double topP;
	private final int seed;
	private final boolean strictJsonSchema;
	private final boolean returnThinking;
	private final boolean logRequests;
	private final boolean logResponses;
	private final Map<String, Object> customParameters;

	private ChatModelConfig(Builder builder) {
		this.baseUrl = builder.baseUrl;
		this.apiKey = builder.apiKey;
		this.modelName = builder.modelName;
		this.temperature = builder.temperature;

		this.timeout = builder.timeout;
		this.maxTokens = builder.maxTokens;
		this.maxRetries = builder.maxRetries;
		this.topP = builder.topP;
		this.seed = builder.seed;
		this.strictJsonSchema = builder.strictJsonSchema;
		this.returnThinking = builder.returnThinking;
		this.logRequests = builder.logRequests;
		this.logResponses = builder.logResponses;
		this.customParameters = Collections.unmodifiableMap(new LinkedHashMap<>(builder.customParameters));
	}

	public static Builder create() {
		return new Builder();
	}

	public static ChatModelConfig load(Path file) throws IOException {
		return new ObjectMapper().readValue(file.toFile(), ChatModelConfig.class);
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

	public Duration timeout() {
		return timeout;
	}

	/**
	 * @return -1 = no limit, up to the model
	 */
	public int maxTokens() {
		return maxTokens;
	}

	public int maxRetries() {
		return maxRetries;
	}

	public double topP() {
		return topP;
	}

	/**
	 * @return -1 = random, a fixed value makes runs more reproducible
	 */
	public int seed() {
		return seed;
	}

	public boolean strictJsonSchema() {
		return strictJsonSchema;
	}

	public boolean returnThinking() {
		return returnThinking;
	}

	public boolean logRequests() {
		return logRequests;
	}

	public boolean logResponses() {
		return logResponses;
	}

	public Map<String, Object> customParameters() {
		return customParameters;
	}

	@Override
	public String toString() {
		return "ChatModelConfig{" + "baseUrl='" + baseUrl + '\'' + ", apiKey='***'" + ", modelName='" + modelName + '\''
				+ ", temperature=" + temperature + ", timeout=" + timeout + ", maxTokens=" + maxTokens
				+ ", maxRetries=" + maxRetries + ", topP=" + topP + ", seed=" + seed
				+ ", strictJsonSchema=" + strictJsonSchema + ", returnThinking=" + returnThinking
				+ ", logRequests=" + logRequests + ", logResponses=" + logResponses
				+ ", customParameters=" + customParameters + '}';
	}

	@JsonPOJOBuilder(withPrefix = "")
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Builder {
		private String baseUrl;
		private String apiKey;
		private String modelName;
		private double temperature;

		private Duration timeout;
		private int maxTokens;
		private int maxRetries;
		private double topP;
		private int seed;
		private boolean strictJsonSchema;
		private boolean returnThinking;
		private boolean logRequests;
		private boolean logResponses;
		private Map<String, Object> customParameters;

		public Builder() {
			super();

			baseUrl = "http://localhost:9090/v1";
			apiKey = "dummy";
			modelName = "test";
			temperature = 0.2;

			timeout = Duration.ofMinutes(5);   // local models can be slow (LangChain4j default: 60 s)
			maxTokens = -1;                    // no limit, up to the model
			maxRetries = 2;
			topP = 1.0;                        // no nucleus sampling cut-off
			seed = -1;                         // random
			strictJsonSchema = false;
			returnThinking = false;
			logRequests = false;
			logResponses = false;
			customParameters = new LinkedHashMap<>();
		}

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

		@JsonDeserialize(using = DurationDeserializer.class)
		public Builder timeout(Duration timeout) {
			this.timeout = timeout;
			return this;
		}

		public Builder maxTokens(int maxTokens) {
			this.maxTokens = maxTokens;
			return this;
		}

		public Builder maxRetries(int maxRetries) {
			this.maxRetries = maxRetries;
			return this;
		}

		public Builder topP(double topP) {
			this.topP = topP;
			return this;
		}

		public Builder seed(int seed) {
			this.seed = seed;
			return this;
		}

		public Builder strictJsonSchema(boolean strictJsonSchema) {
			this.strictJsonSchema = strictJsonSchema;
			return this;
		}

		public Builder returnThinking(boolean returnThinking) {
			this.returnThinking = returnThinking;
			return this;
		}

		public Builder logRequests(boolean logRequests) {
			this.logRequests = logRequests;
			return this;
		}

		public Builder logResponses(boolean logResponses) {
			this.logResponses = logResponses;
			return this;
		}

		public Builder customParameters(Map<String, Object> customParameters) {
			this.customParameters = customParameters!=null ? customParameters : new LinkedHashMap<>();
			return this;
		}

		public ChatModelConfig build() {
			return new ChatModelConfig(this);
		}
	}

	/**
	 * "PT10M" (ISO-8601) or a number of seconds; works without registering
	 * the Jackson JavaTimeModule in the ObjectMapper.
	 */
	public static class DurationDeserializer extends StdDeserializer<Duration> {
		private static final long serialVersionUID = 1L;

		public DurationDeserializer() {
			super(Duration.class);
		}

		@Override
		public Duration deserialize(JsonParser parser, DeserializationContext ctx) throws IOException {
			if (parser.currentToken()==JsonToken.VALUE_NUMBER_INT || parser.currentToken()==JsonToken.VALUE_NUMBER_FLOAT)
				return Duration.ofMillis(Math.round(parser.getDoubleValue()*1000));
			try {
				return Duration.parse(parser.getValueAsString());
			} catch (Exception e) {
				return (Duration)ctx.handleWeirdStringValue(Duration.class, parser.getValueAsString(),
					"expected ISO-8601 duration (e.g. \"PT10M\") or seconds");
			}
		}
	}
}
