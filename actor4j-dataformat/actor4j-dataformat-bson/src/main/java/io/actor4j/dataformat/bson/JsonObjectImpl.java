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
package io.actor4j.dataformat.bson;

import static io.actor4j.dataformat.bson.JsonBsonConverter.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.bson.Document;

import io.actor4j.core.json.JsonArray;
import io.actor4j.core.json.JsonObject;

public class JsonObjectImpl implements JsonObject {
	protected Document document;

	public JsonObjectImpl() {
		super();

		document = new Document();
	}

	// Document (shared), JsonObject, Map or record, everything else is an IllegalArgumentException
	public JsonObjectImpl(Object obj) {
		super();

		document = toDocument(obj);
	}

	// invalid JSON is an IllegalArgumentException
	public JsonObjectImpl(String json) {
		super();

		document = parseObject(json);
	}

	public static JsonObject of() {
		return new JsonObjectImpl();
	}

	public static JsonObject of(String key, Object value) {
		return new JsonObjectImpl().put(key, value);
	}

	public Document getDocument() {
		return document;
	}

	@Override
	public Object getValue(String key) {
		return toJava(document.get(key));
	}

	@Override
	public String getString(String key) {
		return asString(document.get(key));
	}

	@Override
	public Integer getInteger(String key) {
		Number result = asNumber(document.get(key));
		return result!=null ? result.intValue() : null;
	}

	@Override
	public Long getLong(String key) {
		Number result = asNumber(document.get(key));
		return result!=null ? result.longValue() : null;
	}

	@Override
	public Double getDouble(String key) {
		Number result = asNumber(document.get(key));
		return result!=null ? result.doubleValue() : null;
	}

	@Override
	public Boolean getBoolean(String key) {
		return asBoolean(document.get(key));
	}

	@Override
	public JsonObject getJsonObject(String key) {
		return asJsonObject(document.get(key));
	}

	@Override
	public JsonArray getJsonArray(String key) {
		return asJsonArray(document.get(key));
	}

	@Override
	public boolean containsKey(String key) {
		return document.containsKey(key);
	}

	@Override
	public Set<String> fieldNames() {
		return document.keySet();
	}

	@Override
	public JsonObject put(String key, Object value) {
		document.put(key, toJsonBson(value));

		return this;
	}

	@Override
	public Object remove(String key) {
		return toJava(document.remove(key));
	}

	// works with every JsonObject implementation, nested values are copied
	@Override
	public JsonObject mergeIn(JsonObject other) {
		if (other!=null) {
			Document source = toDocument(other);
			for (String key : source.keySet())
				document.put(key, copy(source.get(key)));
		}

		return this;
	}

	@Override
	public String encode() {
		return document.toJson();
	}

	@Override
	public String encodePrettily() {
		return document.toJson(PRETTY);
	}

	// a copy (keeps the order), changes do not affect this object
	@Override
	public Map<String, Object> getMap() {
		return new LinkedHashMap<>(document);
	}

	@Override
	public int size() {
		return document.size();
	}

	@Override
	public JsonObject clear() {
		document.clear();

		return this;
	}

	@Override
	public boolean isEmpty() {
		return document.isEmpty();
	}

	// equal content (numbers by value, nested values deep)
	@Override
	public boolean equals(Object obj) {
		return obj instanceof JsonObjectImpl other && same(document, other.document);
	}

	@Override
	public int hashCode() {
		return hash(document);
	}

	@Override
	public String toString() {
		return encode();
	}

	@Override
	public Object underlyingImpl() {
		return document;
	}
}
