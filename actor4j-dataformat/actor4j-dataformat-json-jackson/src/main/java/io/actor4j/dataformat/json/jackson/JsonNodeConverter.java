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
package io.actor4j.dataformat.json.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Conversion between Java values and Jackson nodes, shared by JsonObjectImpl and JsonArrayImpl.
 */
final class JsonNodeConverter {
	static final ObjectMapper objectMapper;

	static {
		objectMapper = new ObjectMapper();
		objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		objectMapper.registerModule(new JavaTimeModule());
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	}

	private JsonNodeConverter() {
	}

	/**
	 * Java value -> node: wrappers of this module, Jackson nodes, null,
	 * simple values, maps, lists and POJOs/records.
	 */
	static JsonNode toNode(Object value) {
		if (value==null)
			return NullNode.getInstance();
		else if (value instanceof JsonObjectImpl impl)
			return impl.jsonObject;
		else if (value instanceof JsonArrayImpl impl)
			return impl.jsonArray;
		else if (value instanceof JsonNode node)
			return node;
		else
			return objectMapper.valueToTree(value);
	}

	/**
	 * Node -> Java value: objects and arrays as JsonObject / JsonArray (sharing the node),
	 * simple values as String, Integer, Long, Double, Boolean ..., null and missing as null.
	 */
	static Object toJava(JsonNode node) {
		if (node==null || node.isNull() || node.isMissingNode())
			return null;
		else if (node.isObject())
			return new JsonObjectImpl(node);
		else if (node.isArray())
			return new JsonArrayImpl(node);
		else if (node.isTextual())
			return node.textValue();
		else if (node.isBoolean())
			return node.booleanValue();
		else if (node.isNumber())
			return node.numberValue();
		else if (node.isBinary())
			return objectMapper.convertValue(node, byte[].class);
		else
			return node.asText();
	}

	static String encode(JsonNode node) {
		try {
			return objectMapper.writeValueAsString(node);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}

	static String encodePrettily(JsonNode node) {
		try {
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}

	static JsonNode parse(String json) {
		try {
			return objectMapper.readTree(json);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("invalid JSON: " + e.getOriginalMessage(), e);
		}
	}

	// like Vert.x: wrong types are a ClassCastException, null and missing values are null
	static String asString(JsonNode node) {
		if (node==null || node.isNull() || node.isMissingNode())
			return null;
		if (!node.isValueNode())
			throw new ClassCastException("not a simple value: " + node.getNodeType());
		return node.asText();
	}

	static Number asNumber(JsonNode node) {
		if (node==null || node.isNull() || node.isMissingNode())
			return null;
		if (!node.isNumber())
			throw new ClassCastException("not a number: " + node.getNodeType());
		return node.numberValue();
	}

	static Boolean asBoolean(JsonNode node) {
		if (node==null || node.isNull() || node.isMissingNode())
			return null;
		if (!node.isBoolean())
			throw new ClassCastException("not a boolean: " + node.getNodeType());
		return node.booleanValue();
	}
}
