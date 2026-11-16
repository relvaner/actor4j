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

import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import io.actor4j.core.json.JsonArray;
import io.actor4j.core.json.JsonObject;

public class JsonArrayImpl implements JsonArray {
	protected JSONArray jsonArray;

	public JsonArrayImpl() {
		super();

		jsonArray = new JSONArray();
	}

	// JSONArray, JsonArray, Collection or array, everything else is an IllegalArgumentException
	public JsonArrayImpl(Object obj) {
		super();

		jsonArray = toJSONArray(obj);
	}

	public JsonArrayImpl(List<?> list) {
		super();

		jsonArray = toJSONArray(list);
	}

	// invalid JSON is an IllegalArgumentException
	public JsonArrayImpl(String json) {
		super();

		jsonArray = parseArray(json);
	}

	public JsonArrayImpl(JSONArray arr) {
		super();

		if (arr!=null)
			jsonArray = arr;
		else
			jsonArray = new JSONArray();
	}

	@Override
	public Object getValue(int pos) {
		return toJava(jsonArray.opt(pos));
	}

	@Override
	public String getString(int pos) {
		return asString(jsonArray.opt(pos));
	}

	@Override
	public Integer getInteger(int pos) {
		Number result = asNumber(jsonArray.opt(pos));
		return result!=null ? result.intValue() : null;
	}

	@Override
	public Long getLong(int pos) {
		Number result = asNumber(jsonArray.opt(pos));
		return result!=null ? result.longValue() : null;
	}

	@Override
	public Double getDouble(int pos) {
		Number result = asNumber(jsonArray.opt(pos));
		return result!=null ? result.doubleValue() : null;
	}

	@Override
	public Boolean getBoolean(int pos) {
		return asBoolean(jsonArray.opt(pos));
	}

	@Override
	public JsonObject getJsonObject(int pos) {
		return asJsonObject(jsonArray.opt(pos));
	}

	@Override
	public JsonArray getJsonArray(int pos) {
		return asJsonArray(jsonArray.opt(pos));
	}

	@Override
	public JsonArray add(Object value) {
		try {
			jsonArray.put(toJsonOrg(value));
		} catch(JSONException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}

		return this;
	}

	// inserts at pos (the following elements move up), pos>=size appends
	@Override
	public JsonArray add(int pos, Object value) {
		if (pos<0)
			throw new IndexOutOfBoundsException(pos);

		try {
			Object element = toJsonOrg(value);
			int size = jsonArray.length();
			if (pos>=size)
				jsonArray.put(element);
			else {
				jsonArray.put(jsonArray.get(size-1));
				for (int i=size-1; i>pos; i--)
					jsonArray.put(i, jsonArray.get(i-1));
				jsonArray.put(pos, element);
			}
		} catch(JSONException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}

		return this;
	}

	// works with every JsonArray implementation, nested values keep their type
	@Override
	public JsonArray addAll(JsonArray array) {
		if (array!=null) {
			JSONArray source = toJSONArray(array);
			for (int i=0; i<source.length(); i++)
				jsonArray.put(source.opt(i));
		}

		return this;
	}

	@Override
	public JsonArray set(int pos, Object value) {
		try {
			jsonArray.put(pos, toJsonOrg(value));
		} catch(JSONException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}

		return this;
	}

	@Override
	public boolean contains(Object value) {
		Object element = toJsonOrg(value);
		for (int i=0; i<jsonArray.length(); i++)
			if (same(jsonArray.opt(i), element))
				return true;

		return false;
	}

	@Override
	public Object remove(int pos) {
		return toJava(jsonArray.remove(pos));
	}

	@Override
	public int size() {
		return jsonArray.length();
	}

	@Override
	public boolean isEmpty() {
		return jsonArray.isEmpty();
	}

	// a copy, changes do not affect this array
	@Override
	public List<Object> getList() {
		return jsonArray.toList();
	}

	@Override
	public JsonArray clear() {
		jsonArray.clear();

		return this;
	}

	@Override
	public String encode() {
		return jsonArray.toString();
	}

	@Override
	public String encodePrettily() {
		return jsonArray.toString(2);
	}

	// equal content (numbers by value, nested values deep)
	@Override
	public boolean equals(Object obj) {
		return obj instanceof JsonArrayImpl other && jsonArray.similar(other.jsonArray);
	}

	@Override
	public int hashCode() {
		return hash(jsonArray);
	}

	@Override
	public String toString() {
		return jsonArray.toString();
	}

	// nested values wrapped as JsonObject/JsonArray, JSON null as null
	@Override
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

	@Override
	public Object underlyingImpl() {
		return jsonArray;
	}
}
