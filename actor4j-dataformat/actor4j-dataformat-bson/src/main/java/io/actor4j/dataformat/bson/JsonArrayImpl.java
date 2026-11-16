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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.actor4j.core.json.JsonArray;
import io.actor4j.core.json.JsonObject;

public class JsonArrayImpl implements JsonArray {
	protected List<Object> list;

	public JsonArrayImpl() {
		super();

		list = new ArrayList<>();
	}

	// List, Collection, array or JsonArray (copied), everything else is an IllegalArgumentException
	public JsonArrayImpl(Object obj) {
		super();

		list = new ArrayList<>(toList(obj));
	}

	// copy of the list
	public JsonArrayImpl(List<?> list) {
		super();

		this.list = new ArrayList<>(toList(list));
	}

	// invalid JSON is an IllegalArgumentException
	public JsonArrayImpl(String json) {
		super();

		list = parseArray(json);
	}

	// share: the list of a document (getJsonArray), changes arrive in the document
	protected JsonArrayImpl(List<Object> list, boolean share) {
		super();

		this.list = share ? list : new ArrayList<>(list);
	}

	@Override
	public Object getValue(int pos) {
		return toJava(list.get(pos));
	}

	@Override
	public String getString(int pos) {
		return asString(list.get(pos));
	}

	@Override
	public Integer getInteger(int pos) {
		Number result = asNumber(list.get(pos));
		return result!=null ? result.intValue() : null;
	}

	@Override
	public Long getLong(int pos) {
		Number result = asNumber(list.get(pos));
		return result!=null ? result.longValue() : null;
	}

	@Override
	public Double getDouble(int pos) {
		Number result = asNumber(list.get(pos));
		return result!=null ? result.doubleValue() : null;
	}

	@Override
	public Boolean getBoolean(int pos) {
		return asBoolean(list.get(pos));
	}

	@Override
	public JsonObject getJsonObject(int pos) {
		return asJsonObject(list.get(pos));
	}

	@Override
	public JsonArray getJsonArray(int pos) {
		return asJsonArray(list.get(pos));
	}

	@Override
	public JsonArray add(Object value) {
		list.add(toJsonBson(value));

		return this;
	}

	@Override
	public JsonArray add(int pos, Object value) {
		list.add(pos, toJsonBson(value));

		return this;
	}

	// works with every JsonArray implementation
	@Override
	public JsonArray addAll(JsonArray array) {
		if (array!=null)
			list.addAll(toList(array));

		return this;
	}

	@Override
	public JsonArray set(int pos, Object value) {
		list.set(pos, toJsonBson(value));

		return this;
	}

	@Override
	public boolean contains(Object value) {
		Object element = toJsonBson(value);
		for (Object other : list)
			if (same(other, element))
				return true;

		return false;
	}

	@Override
	public Object remove(int pos) {
		return toJava(list.remove(pos));
	}

	@Override
	public int size() {
		return list.size();
	}

	@Override
	public boolean isEmpty() {
		return list.isEmpty();
	}

	// the list itself (live), nested values as Document/List
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
		return encodeArray(list, false);
	}

	@Override
	public String encodePrettily() {
		return encodeArray(list, true);
	}

	// nested values wrapped as JsonObject/JsonArray
	@Override
	public Iterator<Object> iterator() {
		Iterator<Object> iterator = list.iterator();

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

	// equal content (numbers by value, nested values deep)
	@Override
	public boolean equals(Object obj) {
		return obj instanceof JsonArrayImpl other && same(list, other.list);
	}

	@Override
	public int hashCode() {
		return hash(list);
	}

	@Override
	public String toString() {
		return encode();
	}

	@Override
	public Object underlyingImpl() {
		return list;
	}
}
