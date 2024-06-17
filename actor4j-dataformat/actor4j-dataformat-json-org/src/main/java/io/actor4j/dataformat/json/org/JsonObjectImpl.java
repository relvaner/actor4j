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
package io.actor4j.dataformat.json.org;

import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
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
	
	public JsonObjectImpl(Object obj) {
		super();
		
		if (obj instanceof JSONObject)
			jsonObject = (JSONObject)obj;
		else {
			try {
				jsonObject = new JSONObject(obj);
			} catch(JSONException e) {
				jsonObject = new JSONObject();
				e.printStackTrace();
			}
		}
	}
	
	public JsonObjectImpl(String json) {
		super();
		
		try {
			jsonObject = new JSONObject(json);
		} catch(JSONException e) {
			jsonObject = new JSONObject();
			e.printStackTrace();
		}
	}
	
	public static JsonObject of() {
		return new JsonObjectImpl();
	}
	
	public static JsonObject of(String key, Object value) {
		return new JsonObjectImpl().put(key, value);
	}

	@Override
	public Object getValue(String key) {
		Object result = null;
		try {
			result = jsonObject.get(key);
		} catch(JSONException e) {
			e.printStackTrace();
		}
		
		return result;
	}

	@Override
	public String getString(String key) {
		String result = null;
		try {
			result = jsonObject.getString(key);
		} catch(JSONException e) {
			e.printStackTrace();
		}
		
		return result;
	}

	@Override
	public Integer getInteger(String key) {
		Integer result = null;
		try {
			result = jsonObject.getInt(key);
		} catch(JSONException e) {
			e.printStackTrace();
		}
		
		return result;
	}

	@Override
	public Long getLong(String key) {
		Long result = null;
		try {
			result = jsonObject.getLong(key);
		} catch(JSONException e) {
			e.printStackTrace();
		}
		
		return result;
	}

	@Override
	public Double getDouble(String key) {
		Double result = null;
		try {
			result = jsonObject.getDouble(key);
		} catch(JSONException e) {
			e.printStackTrace();
		}
		
		return result;
	}

	@Override
	public Boolean getBoolean(String key) {
		Boolean result = null;
		try {
			result = jsonObject.getBoolean(key);
		} catch(JSONException e) {
			e.printStackTrace();
		}
		
		return result;
	}

	@Override
	public JsonObject getJsonObject(String key) {
		JSONObject result = null;
		try {
			result = jsonObject.getJSONObject(key);
		} catch(JSONException e) {
			e.printStackTrace();
		}
		
		return result!=null ? new JsonObjectImpl(result) : null;
	}

	@Override
	public JsonArray getJsonArray(String key) {
		JSONArray result = null;
		try {
			result = jsonObject.getJSONArray(key);
		} catch(JSONException e) {
			e.printStackTrace();
		}
		
		return result!=null ? new JsonArrayImpl(result) : null;
	}

	@Override
	public boolean containsKey(String key) {
		return jsonObject.has(key);
	}
	
	@Override
	public Set<String> fieldNames() {
		return jsonObject.keySet();
	}

	@Override
	public JsonObject put(String key, Object value) {
		try {
			if (value instanceof JsonObjectImpl impl)
				jsonObject.put(key, impl.jsonObject);
			else if (value instanceof JsonArrayImpl impl)
				jsonObject.put(key, impl.jsonArray);
			else
				jsonObject.put(key, value);
		} catch (JSONException | NullPointerException e) {
			e.printStackTrace();
		}

		return this;
	}

	@Override
	public Object remove(String key) {
		return jsonObject.remove(key);
	}

	@Override
	public JsonObject mergeIn(JsonObject other) {
		if (other!=null && other instanceof JsonObjectImpl other_)
			for (String key : other_.jsonObject.keySet()) {
				try {
					jsonObject.put(key, other.getValue(key));
				} catch (JSONException | NullPointerException e) {
					e.printStackTrace();
				}
			}
			
		return this;
	}

	@Override
	public String encode() {
		return jsonObject.toString();
	}

	@Override
	public String encodePrettily() {
		String result = null;
		try {
			result = jsonObject.toString(2);
		} catch(JSONException e) {
			e.printStackTrace();
		}
		
		return result;
	}

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

	@Override
	public String toString() {
		return jsonObject.toString();
	}
}
