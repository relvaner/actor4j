/*
 * Copyright (c) 2015-2024, David A. Bauer. All rights reserved.
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

import java.util.Map;
import java.util.Set;

import io.actor4j.core.json.JsonArray;
import io.actor4j.core.json.JsonObject;

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
		else {
			
			try {
				jsonObject = io.vertx.core.json.JsonObject.mapFrom(obj);
			} catch(IllegalArgumentException e) {
				jsonObject = new io.vertx.core.json.JsonObject();
				e.printStackTrace();
			}
		}
	}
	
	public JsonObjectImpl(String json) {
		super();

		jsonObject = new io.vertx.core.json.JsonObject(json);
	}
	
	public static JsonObject of() {
		return new JsonObjectImpl();
	}
	
	public static JsonObject of(String key, Object value) {
		return new JsonObjectImpl().put(key, value);
	}

	@Override
	public Object getValue(String key) {
		return jsonObject.getValue(key);
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
	public JsonObject getJsonObject(String key) {
		io.vertx.core.json.JsonObject result = null;
		try {
			result = jsonObject.getJsonObject(key);
		} catch(ClassCastException e) {
			e.printStackTrace();
		}
		
		return result!=null ? new JsonObjectImpl(result) : null;
	}

	@Override
	public JsonArray getJsonArray(String key) {
		io.vertx.core.json.JsonArray result = null;
		try {
			result = jsonObject.getJsonArray(key);
		} catch(ClassCastException e) {
			e.printStackTrace();
		}
		
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
		jsonObject.put(key, value);
		
		return this;
	}

	@Override
	public Object remove(String key) {
		return jsonObject.remove(key);
	}

	@Override
	public JsonObject mergeIn(JsonObject other) {
		jsonObject.mergeIn(jsonObject);
			
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
}
