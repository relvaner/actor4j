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
package io.actor4j.dataformat.json.org;

import static io.actor4j.dataformat.json.org.JsonOrgConverter.*;

import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import io.actor4j.core.json.JsonArray;
import io.actor4j.core.json.JsonObject;

public class JsonObjectImpl implements JsonObject {
	protected JSONObject jsonObject;

	public JsonObjectImpl() {
		super();

		jsonObject = new JSONObject();
	}

	// JSONObject, JsonObject, Map, record or bean, everything else is an IllegalArgumentException
	public JsonObjectImpl(Object obj) {
		super();

		jsonObject = toJSONObject(obj);
	}

	// invalid JSON is an IllegalArgumentException
	public JsonObjectImpl(String json) {
		super();

		jsonObject = parseObject(json);
	}

	public static JsonObject of() {
		return new JsonObjectImpl();
	}

	public static JsonObject of(String key, Object value) {
		return new JsonObjectImpl().put(key, value);
	}

	@Override
	public Object getValue(String key) {
		return toJava(jsonObject.opt(key));
	}

	@Override
	public String getString(String key) {
		return asString(jsonObject.opt(key));
	}

	@Override
	public Integer getInteger(String key) {
		Number result = asNumber(jsonObject.opt(key));
		return result!=null ? result.intValue() : null;
	}

	@Override
	public Long getLong(String key) {
		Number result = asNumber(jsonObject.opt(key));
		return result!=null ? result.longValue() : null;
	}

	@Override
	public Double getDouble(String key) {
		Number result = asNumber(jsonObject.opt(key));
		return result!=null ? result.doubleValue() : null;
	}

	@Override
	public Boolean getBoolean(String key) {
		return asBoolean(jsonObject.opt(key));
	}

	@Override
	public JsonObject getJsonObject(String key) {
		return asJsonObject(jsonObject.opt(key));
	}

	@Override
	public JsonArray getJsonArray(String key) {
		return asJsonArray(jsonObject.opt(key));
	}

	@Override
	public boolean containsKey(String key) {
		return jsonObject.has(key);
	}

	@Override
	public Set<String> fieldNames() {
		return jsonObject.keySet();
	}

	// null is stored as JSON null (the key remains)
	@Override
	public JsonObject put(String key, Object value) {
		try {
			jsonObject.put(key, toJsonOrg(value));
		} catch (JSONException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}

		return this;
	}

	@Override
	public Object remove(String key) {
		return toJava(jsonObject.remove(key));
	}

	// works with every JsonObject implementation, nested values are copied
	@Override
	public JsonObject mergeIn(JsonObject other) {
		if (other!=null) {
			JSONObject source = toJSONObject(other);
			for (String key : source.keySet())
				jsonObject.put(key, copy(source.opt(key)));
		}

		return this;
	}

	@Override
	public String encode() {
		return jsonObject.toString();
	}

	@Override
	public String encodePrettily() {
		return jsonObject.toString(2);
	}

	// a copy, changes do not affect this object
	@Override
	public Map<String,Object> getMap() {
		return jsonObject.toMap();
	}

	@Override
	public int size() {
		return jsonObject.length();
	}

	@Override
	public JsonObject clear() {
		jsonObject.clear();

		return this;
	}

	@Override
	public boolean isEmpty() {
		return jsonObject.isEmpty();
	}

	// equal content (numbers by value, nested values deep)
	@Override
	public boolean equals(Object obj) {
		return obj instanceof JsonObjectImpl other && jsonObject.similar(other.jsonObject);
	}

	@Override
	public int hashCode() {
		return hash(jsonObject);
	}

	@Override
	public String toString() {
		return jsonObject.toString();
	}

	@Override
	public Object underlyingImpl() {
		return jsonObject;
	}
}
