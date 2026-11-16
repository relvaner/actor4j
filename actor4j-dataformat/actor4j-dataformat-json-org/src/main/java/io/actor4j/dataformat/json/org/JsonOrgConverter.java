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

import java.lang.reflect.Array;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.actor4j.core.json.JsonArray;
import io.actor4j.core.json.JsonObject;

// conversion between org.json and actor4j, contract as json-jackson:
// null and missing values are null, wrong types are a ClassCastException, invalid JSON an IllegalArgumentException
final class JsonOrgConverter {
	private JsonOrgConverter() {
	}

	// org.json value -> actor4j (nested values wrapped, JSON null -> null)
	static Object toJava(Object value) {
		if (value==null || value==JSONObject.NULL)
			return null;
		else if (value instanceof JSONObject object)
			return new JsonObjectImpl(object);
		else if (value instanceof JSONArray array)
			return new JsonArrayImpl(array);
		else
			return value;
	}

	// actor4j or Java value -> org.json (null -> JSON null, keeps the key)
	static Object toJsonOrg(Object value) {
		if (value==null)
			return JSONObject.NULL;
		else if (value instanceof JsonObjectImpl impl)
			return impl.jsonObject;
		else if (value instanceof JsonArrayImpl impl)
			return impl.jsonArray;
		else if (value instanceof JsonObject || value instanceof Map || value.getClass().isRecord())
			return toJSONObject(value);
		else if (value instanceof JsonArray || value instanceof Collection || value.getClass().isArray())
			return toJSONArray(value);
		else
			return value;
	}

	// Map, record, JsonObject (any implementation) or bean -> JSONObject
	static JSONObject toJSONObject(Object obj) {
		Objects.requireNonNull(obj, "obj");

		if (obj instanceof JSONObject object)
			return object;
		else if (obj instanceof JsonObjectImpl impl)
			return impl.jsonObject;
		else if (obj instanceof JsonObject other)
			return parseObject(other.encode());
		else if (obj instanceof Map<?, ?> map) {
			JSONObject result = new JSONObject();
			for (Map.Entry<?, ?> entry : map.entrySet())
				result.put(String.valueOf(entry.getKey()), toJsonOrg(entry.getValue()));
			return result;
		}
		else if (obj.getClass().isRecord()) {
			JSONObject result = new JSONObject();
			for (RecordComponent component : obj.getClass().getRecordComponents())
				try {
					result.put(component.getName(), toJsonOrg(component.getAccessor().invoke(obj)));
				} catch (ReflectiveOperationException e) {
					throw new IllegalArgumentException("record component not readable: "+component.getName(), e);
				}
			return result;
		}
		else if (obj instanceof CharSequence || obj instanceof Number || obj instanceof Boolean || obj instanceof Character
				|| obj instanceof Collection || obj.getClass().isArray() || obj instanceof JsonArray || obj instanceof Enum)
			throw new IllegalArgumentException("not a JSON object: "+obj.getClass().getName());
		else
			return new JSONObject(obj); // bean (getters)
	}

	// Collection, array or JsonArray (any implementation) -> JSONArray
	static JSONArray toJSONArray(Object obj) {
		Objects.requireNonNull(obj, "obj");

		if (obj instanceof JSONArray array)
			return array;
		else if (obj instanceof JsonArrayImpl impl)
			return impl.jsonArray;
		else if (obj instanceof JsonArray other)
			return parseArray(other.encode());
		else if (obj instanceof Collection<?> collection) {
			JSONArray result = new JSONArray();
			for (Object value : collection)
				result.put(toJsonOrg(value));
			return result;
		}
		else if (obj.getClass().isArray()) {
			JSONArray result = new JSONArray();
			for (int i=0; i<Array.getLength(obj); i++)
				result.put(toJsonOrg(Array.get(obj, i)));
			return result;
		}
		else
			throw new IllegalArgumentException("not a JSON array: "+obj.getClass().getName());
	}

	static JSONObject parseObject(String json) {
		try {
			return new JSONObject(json);
		} catch (JSONException e) {
			throw new IllegalArgumentException("invalid JSON object: "+e.getMessage(), e);
		}
	}

	static JSONArray parseArray(String json) {
		try {
			return new JSONArray(json);
		} catch (JSONException e) {
			throw new IllegalArgumentException("invalid JSON array: "+e.getMessage(), e);
		}
	}

	// deep copy of nested values (mergeIn must not share subtrees)
	static Object copy(Object value) {
		if (value instanceof JSONObject object)
			return new JSONObject(object.toString());
		else if (value instanceof JSONArray array)
			return new JSONArray(array.toString());
		else
			return value;
	}

	static String asString(Object value) {
		if (value==null || value==JSONObject.NULL)
			return null;
		if (value instanceof JSONObject || value instanceof JSONArray)
			throw new ClassCastException("not a simple value: "+value.getClass().getSimpleName());
		return value.toString();
	}

	static Number asNumber(Object value) {
		if (value==null || value==JSONObject.NULL)
			return null;
		if (!(value instanceof Number))
			throw new ClassCastException("not a number: "+value.getClass().getSimpleName());
		return (Number)value;
	}

	static Boolean asBoolean(Object value) {
		if (value==null || value==JSONObject.NULL)
			return null;
		if (!(value instanceof Boolean))
			throw new ClassCastException("not a boolean: "+value.getClass().getSimpleName());
		return (Boolean)value;
	}

	static JsonObject asJsonObject(Object value) {
		if (value==null || value==JSONObject.NULL)
			return null;
		if (!(value instanceof JSONObject object))
			throw new ClassCastException("not a JSON object: "+value.getClass().getSimpleName());
		return new JsonObjectImpl(object);
	}

	static JsonArray asJsonArray(Object value) {
		if (value==null || value==JSONObject.NULL)
			return null;
		if (!(value instanceof JSONArray array))
			throw new ClassCastException("not a JSON array: "+value.getClass().getSimpleName());
		return new JsonArrayImpl(array);
	}

	// equality of two org.json values: nested via similar, numbers by value (1 == 1L)
	static boolean same(Object a, Object b) {
		if (a instanceof JSONObject object)
			return object.similar(b);
		else if (a instanceof JSONArray array)
			return array.similar(b);
		else if (a instanceof Number x && b instanceof Number y)
			return new BigDecimal(x.toString()).compareTo(new BigDecimal(y.toString()))==0;
		else
			return Objects.equals(a, b);
	}
	
	// hash code consistent with same/similar: numbers by value, nested values deep
	static int hash(Object value) {
		if (value==null || value==JSONObject.NULL)
			return 0;
		else if (value instanceof JSONObject object) {
			int result = 0;
			for (String key : object.keySet())
				result += key.hashCode() ^ hash(object.opt(key));
			return result;
		}
		else if (value instanceof JSONArray array) {
			int result = 1;
			for (int i=0; i<array.length(); i++)
				result = 31*result + hash(array.opt(i));
			return result;
		}
		else if (value instanceof Number number)
			return new BigDecimal(number.toString()).stripTrailingZeros().hashCode();
		else
			return value.hashCode();
	}
}
