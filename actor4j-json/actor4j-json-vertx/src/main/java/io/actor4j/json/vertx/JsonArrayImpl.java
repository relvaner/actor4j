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
package io.actor4j.json.vertx;

import java.util.Iterator;
import java.util.List;

import io.actor4j.core.json.JsonArray;
import io.actor4j.core.json.JsonObject;

public class JsonArrayImpl implements JsonArray {
	protected io.vertx.core.json.JsonArray jsonArray;
	
	public JsonArrayImpl() {
		super();
		
		jsonArray = new io.vertx.core.json.JsonArray();
	}
	
	public JsonArrayImpl(Object obj) {
		super();
		
		if (obj instanceof io.vertx.core.json.JsonArray)
			jsonArray = (io.vertx.core.json.JsonArray)obj;
		else
			jsonArray = new io.vertx.core.json.JsonArray();
	}
	
	public JsonArrayImpl(List<?> list) {
		super();
		
		jsonArray = new io.vertx.core.json.JsonArray(list);
	}
	
	public JsonArrayImpl(String json) {
		super();
		
		jsonArray = new io.vertx.core.json.JsonArray(json);
	}
	
	public JsonArrayImpl(io.vertx.core.json.JsonArray arr) {
		super();
		
		if (arr!=null)
			jsonArray = arr;
		else
			jsonArray = new io.vertx.core.json.JsonArray();
	}

	@Override
	public Object getValue(int pos) {
		return  jsonArray.getValue(pos);
	}

	@Override
	public String getString(int pos) {
		return jsonArray.getString(pos);
	}

	@Override
	public Integer getInteger(int pos) {
		return jsonArray.getInteger(pos);
	}

	@Override
	public Long getLong(int pos) {
		return jsonArray.getLong(pos);
	}

	@Override
	public Double getDouble(int pos) {
		return jsonArray.getDouble(pos);
	}

	@Override
	public Boolean getBoolean(int pos) {
		return jsonArray.getBoolean(pos);
	}

	@Override
	public JsonObject getJsonObject(int pos) {
		io.vertx.core.json.JsonObject result = null;
		try {
			result = jsonArray.getJsonObject(pos);
		} catch(ClassCastException e) {
			e.printStackTrace();
		}
		
		return result!=null ? new JsonObjectImpl(result) : null;
	}

	@Override
	public JsonArray getJsonArray(int pos) {
		io.vertx.core.json.JsonArray result = null;
		try {
			result = jsonArray.getJsonArray(pos);
		} catch(ClassCastException e) {
			e.printStackTrace();
		}
		
		return result!=null ? new JsonArrayImpl(result) : null;
	}

	@Override
	public JsonArray add(Object value) {
		jsonArray.add(value);
		
		return this;
	}

	@Override
	public JsonArray add(int pos, Object value) {
		jsonArray.add(pos, value);
		
		return this;
	}

	@Override
	public JsonArray addAll(JsonArray array) {
		if (array!=null && array instanceof JsonArrayImpl array_)
			jsonArray.addAll(array_.jsonArray);
			
		return this;
	}

	@Override
	public JsonArray set(int pos, Object value) {
		jsonArray.set(pos, value);
		
		return this;
	}

	@Override
	public boolean contains(Object value) {
		return jsonArray.contains(value);
	}

	@Override
	public Object remove(int pos) {
		return jsonArray.remove(pos);
	}

	@Override
	public int size() {
		return jsonArray.size();
	}

	@Override
	public boolean isEmpty() {
		return jsonArray.isEmpty();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Object> getList() {
		return (List<Object>)jsonArray.getList();
	}

	@Override
	public JsonArray clear() {
		jsonArray.clear();
		
		return this;
	}

	@Override
	public String encode() {
		return jsonArray.encode();
	}

	@Override
	public String encodePrettily() {
		return jsonArray.encodePrettily();
	}

	@Override
	public Iterator<Object> iterator() {
		return jsonArray.iterator();
	}
}
