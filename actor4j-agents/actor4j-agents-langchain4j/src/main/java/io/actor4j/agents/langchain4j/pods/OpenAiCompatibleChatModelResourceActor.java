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

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

public abstract class OpenAiCompatibleChatModelResourceActor extends ChatModelResourceActor {
	public ChatModel createAiChatModel(ChatModelConfig chatModelConfig) {
		OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
			.baseUrl(chatModelConfig.baseUrl())
			.apiKey(chatModelConfig.apiKey())
			.modelName(chatModelConfig.modelName())
			.temperature(chatModelConfig.temperature())
			.maxRetries(chatModelConfig.maxRetries())
			.topP(chatModelConfig.topP())
			.strictJsonSchema(chatModelConfig.strictJsonSchema())
			.returnThinking(chatModelConfig.returnThinking())
			.logRequests(chatModelConfig.logRequests())
			.logResponses(chatModelConfig.logResponses());

		// -1 means "not set": no limit / random seed
		if (chatModelConfig.timeout()!=null)
			builder.timeout(chatModelConfig.timeout());
		if (chatModelConfig.maxTokens()!=-1)
			builder.maxTokens(chatModelConfig.maxTokens());
		if (chatModelConfig.seed()!=-1)
			builder.seed(chatModelConfig.seed());
		if (!chatModelConfig.customParameters().isEmpty())
			builder.customParameters(chatModelConfig.customParameters());

		return builder.build();
	}

	public OpenAiCompatibleChatModelResourceActor(ChatModelConfig chatModelConfig) {
		super(chatModelConfig);
	}
}
