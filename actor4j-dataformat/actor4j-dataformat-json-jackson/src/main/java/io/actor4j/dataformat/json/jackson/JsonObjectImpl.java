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

import static io.actor4j.dataformat.json.jackson.JsonNodeConverter.*;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.actor4j.core.json.JsonArray;
import io.actor4j.core.json.JsonObject;

public class JsonObjectImpl implements JsonObject {
	protected ObjectNode jsonObject;

	public JsonObjectImpl() {
		super();

		jsonObject = objectMapper.createObjectNode();
	}

	public JsonObjectImpl(Object obj) {
		super();

		if (obj instanceof ObjectNode node)
			jsonObject = node;
		else if (obj instanceof JsonObjectImpl impl)
			jsonObject = impl.jsonObject;
		else {
			try {
				JsonNode node = toNode(obj);
				if (node instanceof ObjectNode objectNode)
					jsonObject = objectNode;
				else
					throw new IllegalArgumentException("not a JSON object: " + node.getNodeType());
			} catch(IllegalArgumentException e) {
				jsonObject = objectMapper.createObjectNode();
				e.printStackTrace();
			}
		}
	}

	public JsonObjectImpl(String json) {
		super();

		JsonNode node = parse(json);
		if (node instanceof ObjectNode objectNode)
			jsonObject = objectNode;
		else
			throw new IllegalArgumentException("not a JSON object: " + node.getNodeType());
	}

	public static JsonObject of() {
		return new JsonObjectImpl();
	}

	public static JsonObject of(String key, Object value) {
		return new JsonObjectImpl().put(key, value);
	}

	@Override
	public Object getValue(String key) {
		return toJava(jsonObject.get(key));
	}

	@Override
	public String getString(String key) {
		return asString(jsonObject.get(key));
	}

	@Override
	public Integer getInteger(String key) {
		Number result = asNumber(jsonObject.get(key));
		return result!=null ? result.intValue() : null;
	}

	@Override
	public Long getLong(String key) {
		Number result = asNumber(jsonObject.get(key));
		return result!=null ? result.longValue() : null;
	}

	@Override
	public Double getDouble(String key) {
		Number result = asNumber(jsonObject.get(key));
		return result!=null ? result.doubleValue() : null;
	}

	@Override
	public Boolean getBoolean(String key) {
		return asBoolean(jsonObject.get(key));
	}

	@Override
	public JsonObject getJsonObject(String key) {
		JsonNode result = jsonObject.get(key);
		if (result!=null && !result.isNull() && !result.isObject()) {
			new ClassCastException("not a JSON object: " + result.getNodeType()).printStackTrace();
			return null;
		}

		return result!=null && result.isObject() ? new JsonObjectImpl(result) : null;
	}

	@Override
	public JsonArray getJsonArray(String key) {
		JsonNode result = jsonObject.get(key);
		if (result!=null && !result.isNull() && !result.isArray()) {
			new ClassCastException("not a JSON array: " + result.getNodeType()).printStackTrace();
			return null;
		}

		return result!=null && result.isArray() ? new JsonArrayImpl(result) : null;
	}

	@Override
	public boolean containsKey(String key) {
		return jsonObject.has(key);
	}

	@Override
	public Set<String> fieldNames() {
		Set<String> result = new LinkedHashSet<>();
		jsonObject.fieldNames().forEachRemaining(result::add);

		return result;
	}

	@Override
	public JsonObject put(String key, Object value) {
		jsonObject.set(key, toNode(value));

		return this;
	}

	@Override
	public Object remove(String key) {
		return toJava(jsonObject.remove(key));
	}

	@Override
	public JsonObject mergeIn(JsonObject other) {
		if (other!=null)
			for (String key : other.fieldNames())
				put(key, other.getValue(key));

		return this;
	}

	@Override
	public String encode() {
		return JsonNodeConverter.encode(jsonObject);
	}

	@Override
	public String encodePrettily() {
		return JsonNodeConverter.encodePrettily(jsonObject);
	}

	/**
	 * @return a copy as plain Java map (nested objects and arrays as Map and List)
	 */
	@Override
	public Map<String,Object> getMap() {
		return objectMapper.convertValue(jsonObject, new TypeReference<Map<String,Object>>() {});
	}

	@Override
	public int size() {
		return jsonObject.size();
	}

	@Override
	public JsonObject clear() {
		jsonObject.removeAll();

		return this;
	}

	@Override
	public boolean isEmpty() {
		return jsonObject.isEmpty();
	}

	@Override
	public Object underlyingImpl() {
		return jsonObject;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof JsonObjectImpl other && jsonObject.equals(other.jsonObject);
	}

	@Override
	public int hashCode() {
		return jsonObject.hashCode();
	}

	@Override
	public String toString() {
		return encode();
	}
}
