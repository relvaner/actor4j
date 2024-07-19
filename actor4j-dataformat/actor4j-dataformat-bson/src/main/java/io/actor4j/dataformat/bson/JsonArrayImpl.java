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
package io.actor4j.dataformat.bson;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bson.Document;

import io.actor4j.core.json.JsonArray;
import io.actor4j.core.json.JsonObject;

public class JsonArrayImpl implements JsonArray {
	protected List<Object> list;
	
	public JsonArrayImpl() {
		super();
		
		list = new ArrayList<>();
	}
	
	public JsonArrayImpl(List<?> list) {
		super();
		
		this.list.addAll(list);
	}

	@Override
	public Object getValue(int pos) {
		return list.get(pos);
	}

	@Override
	public String getString(int pos) {
		return (String)list.get(pos);
	}

	@Override
	public Integer getInteger(int pos) {
		return (Integer)list.get(pos);
	}

	@Override
	public Long getLong(int pos) {
		return (Long)list.get(pos);
	}

	@Override
	public Double getDouble(int pos) {
		return (Double)list.get(pos);
	}

	@Override
	public Boolean getBoolean(int pos) {
		return (Boolean)list.get(pos);
	}

	@Override
	public JsonObject getJsonObject(int pos) {
		Document result = null;
		result = (Document)list.get(pos);
		
		return result!=null ? new JsonObjectImpl(result) : null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public JsonArray getJsonArray(int pos) {
		List<Object> result = null;
		result = (List<Object>)list.get(pos);
		
		return result!=null ? new JsonArrayImpl(result) : null;
	}

	@Override
	public JsonArray add(Object value) {
		list.add(value);
		
		return this;
	}

	@Override
	public JsonArray add(int pos, Object value) {
		list.add(pos, value);
		
		return this;
	}

	@Override
	public JsonArray addAll(JsonArray array) {
		if (array!=null && array instanceof JsonArrayImpl array_)
			list.addAll(array_.list);
			
		return this;
	}

	@Override
	public JsonArray set(int pos, Object value) {
		list.set(pos, value);
		
		return this;
	}

	@Override
	public boolean contains(Object value) {
		return list.contains(value);
	}

	@Override
	public Object remove(int pos) {
		return list.remove(pos);
	}

	@Override
	public int size() {
		return list.size();
	}

	@Override
	public boolean isEmpty() {
		return list.isEmpty();
	}

	@Override
	public List<Object> getList() {
		return list;
	}

	@Override
	public JsonArray clear() {
		list.clear();
		
		return this;
	}

	@Override
	public String encode() {
		return list.toString();
	}

	@Override
	public String encodePrettily() {
		return list.toString();
	}

	@Override
	public Iterator<Object> iterator() {
		return list.iterator();
	}

	@Override
	public Object underlyingImpl() {
		return list;
	}
}
