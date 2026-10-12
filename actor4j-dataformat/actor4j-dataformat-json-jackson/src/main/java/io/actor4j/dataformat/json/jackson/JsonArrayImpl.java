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

import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import io.actor4j.core.json.JsonArray;
import io.actor4j.core.json.JsonObject;

public class JsonArrayImpl implements JsonArray {
	protected ArrayNode jsonArray;

	public JsonArrayImpl() {
		super();

		jsonArray = objectMapper.createArrayNode();
	}

	public JsonArrayImpl(Object obj) {
		super();

		if (obj instanceof ArrayNode node)
			jsonArray = node;
		else if (obj instanceof JsonArrayImpl impl)
			jsonArray = impl.jsonArray;
		else
			jsonArray = objectMapper.createArrayNode();
	}

	public JsonArrayImpl(List<?> list) {
		super();

		jsonArray = objectMapper.createArrayNode();
		if (list!=null)
			for (Object value : list)
				jsonArray.add(toNode(value));
	}

	public JsonArrayImpl(String json) {
		super();

		JsonNode node = parse(json);
		if (node instanceof ArrayNode arrayNode)
			jsonArray = arrayNode;
		else
			throw new IllegalArgumentException("not a JSON array: " + node.getNodeType());
	}

	@Override
	public Object getValue(int pos) {
		return toJava(jsonArray.get(pos));
	}

	@Override
	public String getString(int pos) {
		return asString(jsonArray.get(pos));
	}

	@Override
	public Integer getInteger(int pos) {
		Number result = asNumber(jsonArray.get(pos));
		return result!=null ? result.intValue() : null;
	}

	@Override
	public Long getLong(int pos) {
		Number result = asNumber(jsonArray.get(pos));
		return result!=null ? result.longValue() : null;
	}

	@Override
	public Double getDouble(int pos) {
		Number result = asNumber(jsonArray.get(pos));
		return result!=null ? result.doubleValue() : null;
	}

	@Override
	public Boolean getBoolean(int pos) {
		return asBoolean(jsonArray.get(pos));
	}

	@Override
	public JsonObject getJsonObject(int pos) {
		JsonNode result = jsonArray.get(pos);
		if (result!=null && !result.isNull() && !result.isObject()) {
			new ClassCastException("not a JSON object: " + result.getNodeType()).printStackTrace();
			return null;
		}

		return result!=null && result.isObject() ? new JsonObjectImpl(result) : null;
	}

	@Override
	public JsonArray getJsonArray(int pos) {
		JsonNode result = jsonArray.get(pos);
		if (result!=null && !result.isNull() && !result.isArray()) {
			new ClassCastException("not a JSON array: " + result.getNodeType()).printStackTrace();
			return null;
		}

		return result!=null && result.isArray() ? new JsonArrayImpl(result) : null;
	}

	@Override
	public JsonArray add(Object value) {
		jsonArray.add(toNode(value));

		return this;
	}

	@Override
	public JsonArray add(int pos, Object value) {
		jsonArray.insert(pos, toNode(value));

		return this;
	}

	@Override
	public JsonArray addAll(JsonArray array) {
		if (array!=null)
			for (Object value : array)
				add(value);

		return this;
	}

	@Override
	public JsonArray set(int pos, Object value) {
		jsonArray.set(pos, toNode(value));

		return this;
	}

	@Override
	public boolean contains(Object value) {
		JsonNode node = toNode(value);
		for (JsonNode element : jsonArray)
			if (element.equals(node))
				return true;

		return false;
	}

	@Override
	public Object remove(int pos) {
		return toJava(jsonArray.remove(pos));
	}

	@Override
	public int size() {
		return jsonArray.size();
	}

	@Override
	public boolean isEmpty() {
		return jsonArray.isEmpty();
	}

	/**
	 * @return a copy as plain Java list (nested objects and arrays as Map and List)
	 */
	@Override
	public List<Object> getList() {
		return objectMapper.convertValue(jsonArray, new TypeReference<List<Object>>() {});
	}

	@Override
	public JsonArray clear() {
		jsonArray.removeAll();

		return this;
	}

	@Override
	public String encode() {
		return JsonNodeConverter.encode(jsonArray);
	}

	@Override
	public String encodePrettily() {
		return JsonNodeConverter.encodePrettily(jsonArray);
	}

	/**
	 * Elements as in {@link #getValue(int)}: objects and arrays as JsonObject / JsonArray.
	 */
	@Override
	public Iterator<Object> iterator() {
		Iterator<JsonNode> iterator = jsonArray.iterator();

		return new Iterator<Object>() {
			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public Object next() {
				return toJava(iterator.next());
			}
		};
	}

	@Override
	public Object underlyingImpl() {
		return jsonArray;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof JsonArrayImpl other && jsonArray.equals(other.jsonArray);
	}

	@Override
	public int hashCode() {
		return jsonArray.hashCode();
	}

	@Override
	public String toString() {
		return encode();
	}
}
