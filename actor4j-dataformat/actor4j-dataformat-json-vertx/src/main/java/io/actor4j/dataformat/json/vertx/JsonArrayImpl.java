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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import io.actor4j.core.json.JsonArray;
import io.actor4j.core.json.JsonObject;

import static io.actor4j.dataformat.json.vertx.JsonVertxConverter.*;

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
		else if (obj instanceof Collection<?> collection)
			jsonArray = copy(collection);
		else if (obj!=null && obj.getClass().isArray()) {
			List<Object> list = new ArrayList<>();
			for (int i=0; i<java.lang.reflect.Array.getLength(obj); i++)
				list.add(java.lang.reflect.Array.get(obj, i));
			jsonArray = copy(list);
		}
		else
			throw new IllegalArgumentException("not a JSON array: "+(obj!=null ? obj.getClass().getName() : null));
	}
	
	public JsonArrayImpl(List<?> list) {
		super();
		
		jsonArray = copy(list);
	}
	
	// invalid JSON is an IllegalArgumentException
	public JsonArrayImpl(String json) {
		super();

		jsonArray = parseArray(json);
	}
	
	public JsonArrayImpl(io.vertx.core.json.JsonArray arr) {
		super();
		
		if (arr!=null)
			jsonArray = arr;
		else
			jsonArray = new io.vertx.core.json.JsonArray();
	}
	
	protected static io.vertx.core.json.JsonArray copy(Collection<?> collection) {
		List<Object> result = new ArrayList<>();
		for (Object value : collection)
			result.add(toJsonVertx(value));
		
		return new io.vertx.core.json.JsonArray(result);
	}

	@Override
	public Object getValue(int pos) {
		return toJava(jsonArray.getValue(pos));
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
	// a wrong type is a ClassCastException (as the other getters)
	public JsonObject getJsonObject(int pos) {
		io.vertx.core.json.JsonObject result = jsonArray.getJsonObject(pos);

		return result!=null ? new JsonObjectImpl(result) : null;
	}

	@Override
	public JsonArray getJsonArray(int pos) {
		io.vertx.core.json.JsonArray result = jsonArray.getJsonArray(pos);

		return result!=null ? new JsonArrayImpl(result) : null;
	}

	@Override
	public JsonArray add(Object value) {
		jsonArray.add(toJsonVertx(value));

		return this;
	}

	@Override
	public JsonArray add(int pos, Object value) {
		jsonArray.add(pos, toJsonVertx(value));

		return this;
	}

	@Override
	// works with every JsonArray implementation
	public JsonArray addAll(JsonArray array) {
		if (array!=null)
			for (Object value : array)
				add(value);

		return this;
	}

	@Override
	public JsonArray set(int pos, Object value) {
		jsonArray.set(pos, toJsonVertx(value));

		return this;
	}

	@Override
	public boolean contains(Object value) {
		return jsonArray.contains(toJsonVertx(value));
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
	// nested values wrapped as JsonObject/JsonArray
	public Iterator<Object> iterator() {
		Iterator<Object> iterator = jsonArray.iterator();

		return new Iterator<Object>() {
			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public Object next() {
				return toJava(iterator.next());
			}

			@Override
			public void remove() {
				iterator.remove();
			}
		};
	}

	// equal content (vert.x compares numbers by value)
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
		return jsonArray.encode();
	}

	@Override
	public Object underlyingImpl() {
		return jsonArray;
	}
}
