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
package io.actor4j.agents.langchain4j.utils;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.langchain4j.internal.JsonSchemaElementUtils;
import dev.langchain4j.model.chat.request.json.JsonRawSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.service.output.JsonSchemas;

/**
 * Creates JSON schemas (standard JSON Schema, e.g. {"type": "object", "properties": {...}})
 * for structured answers of a chat model: from files or text (changeable without compiling)
 * or from records (when Java code evaluates the fields).
 */
public final class JsonSchemaUtils {
	// name of a JSON schema, as required by OpenAI compatible APIs
	public static final Pattern SCHEMA_NAME = Pattern.compile("[A-Za-z0-9_-]{1,64}");

	private JsonSchemaUtils() {
	}

	/**
	 * Loads a JSON schema from a file. The name is the file name without ".json",
	 * e.g. "plan.json" -> "plan".
	 */
	public static JsonSchema load(Path file) throws IOException {
		return fromJson(nameOf(file), Files.readString(file));
	}

	/**
	 * Loads all JSON schemas (*.json) of a directory.
	 *
	 * @return name -> schema
	 */
	public static Map<String, JsonSchema> loadAll(Path directory) throws IOException {
		Map<String, JsonSchema> result = new HashMap<>();
		try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*.json")) {
			for (Path file : files) {
				JsonSchema schema = load(file);
				result.put(schema.name(), schema);
			}
		}

		return result;
	}

	/**
	 * Creates a JSON schema from text, e.g. from a block of a configuration file.
	 */
	public static JsonSchema fromJson(String name, String json) throws IOException {
		if (name==null || !SCHEMA_NAME.matcher(name).matches())
			throw new IllegalArgumentException("invalid JSON schema name '" + name + "', allowed: letters, digits, '_' and '-'");
		if (!(new ObjectMapper().readTree(json) instanceof ObjectNode))
			throw new IllegalArgumentException("JSON schema '" + name + "' must be a JSON object");

		return JsonSchema.builder()
			.name(name)
			.rootElement(JsonRawSchema.from(json))
			.build();
	}

	/**
	 * Creates a JSON schema from a record (or class), named after the class, e.g. Plan -> "Plan".
	 * Field descriptions with {@code @Description}, Java enums become "enum".
	 * All fields are required and no further fields are allowed (strict, like a schema file).
	 */
	public static JsonSchema from(Class<?> type) throws IOException {
		return from(type.getSimpleName(), type);
	}

	public static JsonSchema from(String name, Class<?> type) throws IOException {
		JsonSchema generated = JsonSchemas.jsonSchemaFrom(type)
			.orElseThrow(() -> new IllegalArgumentException("no JSON schema for " + type.getName() + " (record or class expected)"));

		Map<String, Object> map = JsonSchemaElementUtils.toMap(generated.rootElement());
		makeStrict(map);

		return fromJson(name, new ObjectMapper().writeValueAsString(map));
	}

	@SuppressWarnings("unchecked")
	protected static void makeStrict(Object element) {
		if (element instanceof Map<?, ?> map) {
			if ("object".equals(map.get("type")) && map.get("properties") instanceof Map<?, ?> properties) {
				((Map<String, Object>)map).put("required", new ArrayList<>(properties.keySet()));
				((Map<String, Object>)map).putIfAbsent("additionalProperties", false);
			}
			map.values().forEach(JsonSchemaUtils::makeStrict);
		}
		else if (element instanceof List<?> list)
			list.forEach(JsonSchemaUtils::makeStrict);
	}

	public static String nameOf(Path file) {
		String fileName = file.getFileName().toString();

		return fileName.endsWith(".json") ? fileName.substring(0, fileName.length()-".json".length()) : fileName;
	}
}
