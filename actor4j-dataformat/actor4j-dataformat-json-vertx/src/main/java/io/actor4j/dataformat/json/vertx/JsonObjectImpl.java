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
package io.actor4j.dataformat.json.vertx;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import io.actor4j.core.json.JsonArray;
import io.actor4j.core.json.JsonObject;
import io.vertx.core.json.DecodeException;

import static io.actor4j.dataformat.json.vertx.JsonVertxConverter.*;

public class JsonObjectImpl implements JsonObject {
	protected io.vertx.core.json.JsonObject jsonObject;
	
	public JsonObjectImpl() {
		super();
		
		jsonObject = new io.vertx.core.json.JsonObject();
	}

	public JsonObjectImpl(Object obj) {
		super();
		
		if (obj instanceof io.vertx.core.json.JsonObject)
			jsonObject = (io.vertx.core.json.JsonObject)obj;
		else if (obj instanceof Map<?, ?> map) {
			Map<String, Object> copy = new LinkedHashMap<>();
			for (Map.Entry<?, ?> entry : map.entrySet())
				if (entry.getKey() instanceof String key)
					copy.put(key, entry.getValue() instanceof JsonObjectImpl impl ? impl.jsonObject
						: entry.getValue() instanceof JsonArrayImpl impl ? impl.jsonArray : entry.getValue());
				else
					throw new IllegalArgumentException("JSON keys must be strings: "+entry.getKey());
			jsonObject = new io.vertx.core.json.JsonObject(copy);
		}
		else {
			try {
				jsonObject = io.vertx.core.json.JsonObject.mapFrom(obj);
			} catch(DecodeException e) {
				throw new IllegalStateException("mapping of "+(obj!=null ? obj.getClass().getName() : null)+" requires jackson-databind on the classpath", e);
			}
		}
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
		return toJava(jsonObject.getValue(key));
	}

	@Override
	public String getString(String key) {
		return jsonObject.getString(key);
	}

	@Override
	public Integer getInteger(String key) {
		return jsonObject.getInteger(key);
	}

	@Override
	public Long getLong(String key) {
		return jsonObject.getLong(key);
	}

	@Override
	public Double getDouble(String key) {
		return jsonObject.getDouble(key);
	}

	@Override
	public Boolean getBoolean(String key) {
		return jsonObject.getBoolean(key);
	}

	@Override
	// a wrong type is a ClassCastException (as the other getters)
	public JsonObject getJsonObject(String key) {
		io.vertx.core.json.JsonObject result = jsonObject.getJsonObject(key);

		return result!=null ? new JsonObjectImpl(result) : null;
	}

	@Override
	public JsonArray getJsonArray(String key) {
		io.vertx.core.json.JsonArray result = jsonObject.getJsonArray(key);

		return result!=null ? new JsonArrayImpl(result) : null;
	}

	@Override
	public boolean containsKey(String key) {
		return jsonObject.containsKey(key);
	}
	
	@Override
	public Set<String> fieldNames() {
		return jsonObject.fieldNames();
	}

	@Override
	public JsonObject put(String key, Object value) {
		jsonObject.put(key, toJsonVertx(value));

		return this;
	}

	@Override
	public Object remove(String key) {
		return toJava(jsonObject.remove(key));
	}

	@Override
	// works with every JsonObject implementation, nested values are copied
	public JsonObject mergeIn(JsonObject other) {
		if (other!=null)
			for (String key : other.fieldNames())
				jsonObject.put(key, copy(toJsonVertx(other.getValue(key))));

		return this;
	}

	@Override
	public String encode() {
		return jsonObject.encode();
	}

	@Override
	public String encodePrettily() {
		return jsonObject.encodePrettily();
	}

	@Override
	public Map<String,Object> getMap() {
		return jsonObject.getMap();
	}
	
	@Override
	public int size() {
		return jsonObject.size();
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

	// equal content (vert.x compares numbers by value)
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
		return jsonObject.encode();
	}

	@Override
	public Object underlyingImpl() {
		return jsonObject;
	}
}
