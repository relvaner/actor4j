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
		return OpenAiChatModel.builder()
			.baseUrl(chatModelConfig.baseUrl())
			.apiKey(chatModelConfig.apiKey())
			.modelName(chatModelConfig.modelName())
			.temperature(chatModelConfig.temperature())
			.build();
	}
	
	public abstract void createAiServices(ChatModel chatModel);
	
	public OpenAiCompatibleChatModelResourceActor(ChatModelConfig chatModelConfig) {
		super(chatModelConfig);
	}
}
