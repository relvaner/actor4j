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
package io.actor4j.json.bson;

import java.util.HashMap;
import java.util.List;
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
	
	public JsonObjectImpl(Object obj) {
		super();
		
		if (obj instanceof Document)
			document = (Document)obj;
		else 
			throw new UnsupportedOperationException();
	}
	
	public JsonObjectImpl(String json) {
		super();

		document = Document.parse(json);
	}
	
	public static JsonObject of() {
		return new JsonObjectImpl();
	}
	
	public static JsonObject of(String key, Object value) {
		return new JsonObjectImpl().put(key, value);
	}

	@Override
	public Object getValue(String key) {
		return document.get(key);
	}

	@Override
	public String getString(String key) {
		return document.getString(key);
	}

	@Override
	public Integer getInteger(String key) {
		return document.getInteger(key);
	}

	@Override
	public Long getLong(String key) {
		return document.getLong(key);
	}

	@Override
	public Double getDouble(String key) {
		return document.getDouble(key);
	}

	@Override
	public Boolean getBoolean(String key) {
		return document.getBoolean(key);
	}

	@Override
	public JsonObject getJsonObject(String key) {
		Document result = null;
		try {
			result = document.get(key, Document.class);
		}
		catch(ClassCastException e) {
			e.printStackTrace();
		}

		return result!=null ? new JsonObjectImpl(result) : null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public JsonArray getJsonArray(String key) {
		List<Object> result = null;
		try {
			result = document.get(key, List.class);
		} catch(ClassCastException e) {
			e.printStackTrace();
		}

		return result!=null ? new JsonArrayImpl(result) : null;
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
		document.put(key, value);
		
		return this;
	}

	@Override
	public Object remove(String key) {
		return document.remove(key);
	}

	@Override
	public JsonObject mergeIn(JsonObject other) {
		if (other!=null && other instanceof JsonObjectImpl other_)
			for (String key : other_.document.keySet())
				document.put(key, other.getValue(key));
			
		return this;
	}

	@Override
	public String encode() {
		return document.toJson();
	}

	@Override
	public String encodePrettily() {
		return document.toJson();
	}

	@Override
	public Map<String, Object> getMap() {
		Map<String, Object> result = new HashMap<>();
		
		for (String key : document.keySet())
			result.put(key, document.get(key));
		
		return result;
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
}
