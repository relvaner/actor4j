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

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import io.actor4j.agents.langchain4j.pods.OpenAICompatibleAgentPod;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.functions.pods.FunctionPodContext;
import io.actor4j.functions.pods.PodFunction;

public class ExampleAgentFuctionPod extends OpenAICompatibleAgentPod {
	public static final String BASE_URL = "http://localhost:9090/v1";
	public static final String API_KEY = "dummy";
	public static final String MODEL_NAME = "test";
	public static final double TEMPERATURE = 0.2; 
	
	protected AnalystAgent agent;
	
	record TaskAnalysis(String summary, List<String> steps, double confidence) {
	}

	interface AnalystAgent {
		@SystemMessage("You are a precise system analyst.")
		TaskAnalysis analyze(String task);
	}

	@Override
	public String domain() {
		return "ExampleAgentFuctionPod";
	}

	@Override
	public OpenAiChatModel createOpenAiChatModel() {
		return OpenAiChatModel.builder()
			.baseUrl(BASE_URL)
			.apiKey(API_KEY)
			.modelName(MODEL_NAME)
			.temperature(TEMPERATURE)
			.build();
	}

	@Override
	public PodFunction createPodFunction(FunctionPodContext contextFunction) {
		return new PodFunction(contextFunction) {
			@Override
			public Reply handle(ActorMessage<?> message) {
				TaskAnalysis result = agent.analyze(message.valueAsString());
				return Reply.of(result, 0);
			}
		};
	}
	
	public void preStart(FunctionPodContext contextFunction) {
		super.preStart(contextFunction);
		
		agent = AiServices.create(AnalystAgent.class, model);
	}
}
