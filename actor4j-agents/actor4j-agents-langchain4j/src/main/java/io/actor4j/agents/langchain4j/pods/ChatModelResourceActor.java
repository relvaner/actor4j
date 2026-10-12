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

import static dev.langchain4j.model.chat.request.ResponseFormatType.JSON;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import io.actor4j.core.actors.ResourceActor;

public abstract class ChatModelResourceActor extends ResourceActor {
	protected ChatModel model;
	protected ChatModelConfig chatModelConfig;
	protected Map<String, JsonSchema> jsonSchemas; // stateful resource actor -> no concurrent access

	public abstract ChatModel createAiChatModel(ChatModelConfig chatModelConfig);

	/**
	 * Optional: for typed agents with LangChain4j AiServices (prompts as annotations).
	 * Not needed when using {@link #chat(String, String, JsonSchema)}.
	 */
	public void createAiServices(ChatModel chatModel) {
		// empty
	}

	public ChatModelResourceActor(ChatModelConfig chatModelConfig) {
		super(true);
		this.chatModelConfig = chatModelConfig;
	}

	@Override
	public void preStart() {
		jsonSchemas = new HashMap<>();
		
		model = createAiChatModel(chatModelConfig);
		createAiServices(model);
	}

	/**
	 * Calls the model directly, e.g. with prompts from a configuration file.
	 *
	 * @param system system message, may be null
	 * @param user user message
	 * @param schema requested JSON schema of the answer, null for free text
	 * @return the answer (JSON text, if a schema was given)
	 */
	public String chat(String system, String user, JsonSchema schema) {
		ChatRequest.Builder request = ChatRequest.builder();
		if (system!=null)
			request.messages(SystemMessage.from(system), UserMessage.from(user));
		else
			request.messages(UserMessage.from(user));

		if (schema!=null)
			request.responseFormat(ResponseFormat.builder().type(JSON).jsonSchema(schema).build());

		return model.chat(request.build()).aiMessage().text();
	}

	/**
	 * Calls the model with free text as answer.
	 */
	public String chat(String system, String user) {
		return chat(system, user, (JsonSchema)null);
	}

	/**
	 * Calls the model with a registered JSON schema (see {@link #addJsonSchema(JsonSchema)}).
	 *
	 * @param schemaName name of the schema
	 * @return the answer as JSON text
	 */
	public String chatJson(String system, String user, String schemaName) {
		JsonSchema schema = jsonSchemas.get(schemaName);
		if (schema==null)
			throw new IllegalArgumentException("unknown JSON schema '" + schemaName + "', registered: " + jsonSchemas.keySet());

		return chat(system, user, schema);
	}

	/**
	 * Registers a JSON schema under its name, e.g. loaded with JsonSchemaUtils.
	 */
	public void addJsonSchema(JsonSchema schema) {
		jsonSchemas.put(schema.name(), schema);
	}

	public void addJsonSchemas(Map<String, JsonSchema> schemas) {
		jsonSchemas.putAll(schemas);
	}

	public JsonSchema jsonSchema(String name) {
		return jsonSchemas.get(name);
	}

	public Set<String> jsonSchemaNames() {
		return Set.copyOf(jsonSchemas.keySet());
	}
}
