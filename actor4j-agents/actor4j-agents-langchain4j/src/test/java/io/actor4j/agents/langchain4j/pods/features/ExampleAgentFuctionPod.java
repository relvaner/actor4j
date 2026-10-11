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
package io.actor4j.agents.langchain4j.pods.features;

import java.util.List;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import io.actor4j.agents.langchain4j.pods.ChatModelConfig;
import io.actor4j.agents.langchain4j.pods.ChatModelPod;
import io.actor4j.agents.langchain4j.pods.ChatModelResourceActor;
import io.actor4j.agents.langchain4j.pods.OpenAiCompatibleChatModelResourceActor;
import io.actor4j.core.messages.ActorMessage;

public class ExampleAgentFuctionPod extends ChatModelPod {	
	public ExampleAgentFuctionPod(String domain, ChatModelConfig chatModelConfig) {
		super(domain, chatModelConfig);
	}

	record TaskAnalysis(String summary, List<String> steps, double confidence) {
	}

	interface AnalystAgent {
		@SystemMessage("You are a precise system analyst.")
		TaskAnalysis analyze(String task);
	}
	
	@Override
	public ChatModelResourceActor createChatModelResourceActor(ChatModelConfig chatModelConfig) {
		return new OpenAiCompatibleChatModelResourceActor(chatModelConfig) {
			protected AnalystAgent agent;

			@Override
			public void createAiServices(ChatModel chatModel) {
				agent = AiServices.create(AnalystAgent.class, chatModel);
			}

			@Override
			public void receive(ActorMessage<?> message) {
				System.out.println("ChatModelResourceActor.receive: " + message.value());
				
				TaskAnalysis result = null;
				try {
					result = agent.analyze(message.valueAsString());
				}
				catch(Exception e) {
					e.printStackTrace();
				}
				
				if (result!=null)
					tell(result, 0, message.source(), message.interaction());
			}
		};
	}
}
