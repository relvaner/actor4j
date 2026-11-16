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

import java.lang.reflect.Array;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.bson.Document;
import org.bson.json.JsonParseException;
import org.bson.json.JsonWriterSettings;

import io.actor4j.core.json.JsonArray;
import io.actor4j.core.json.JsonObject;

// conversion between BSON (Document, List) and actor4j, contract as json-jackson:
// null and missing values are null, wrong types are a ClassCastException, invalid JSON an IllegalArgumentException
final class JsonBsonConverter {
	static final JsonWriterSettings PRETTY = JsonWriterSettings.builder().indent(true).build();

	private JsonBsonConverter() {
	}

	// BSON value -> actor4j (nested values wrapped, the list is shared)
	@SuppressWarnings("unchecked")
	static Object toJava(Object value) {
		if (value instanceof Document document)
			return new JsonObjectImpl(document);
		else if (value instanceof List<?> list)
			return new JsonArrayImpl((List<Object>)list, true);
		else
			return value;
	}

	// actor4j or Java value -> BSON
	static Object toJsonBson(Object value) {
		if (value==null)
			return null;
		else if (value instanceof JsonObjectImpl impl)
			return impl.document;
		else if (value instanceof JsonArrayImpl impl)
			return impl.list;
		else if (value instanceof Document || value instanceof JsonObject || value instanceof Map || value.getClass().isRecord())
			return toDocument(value);
		else if (value instanceof JsonArray || value instanceof Collection || (value.getClass().isArray() && !(value instanceof byte[])))
			return toList(value);
		else
			return value;
	}

	// Document, JsonObject (any implementation), Map or record -> Document
	static Document toDocument(Object obj) {
		Objects.requireNonNull(obj, "obj");

		if (obj instanceof Document document)
			return document;
		else if (obj instanceof JsonObjectImpl impl)
			return impl.document;
		else if (obj instanceof JsonObject other)
			return parseObject(other.encode());
		else if (obj instanceof Map<?, ?> map) {
			Document result = new Document();
			for (Map.Entry<?, ?> entry : map.entrySet())
				result.put(String.valueOf(entry.getKey()), toJsonBson(entry.getValue()));
			return result;
		}
		else if (obj.getClass().isRecord()) {
			Document result = new Document();
			for (RecordComponent component : obj.getClass().getRecordComponents())
				try {
					result.put(component.getName(), toJsonBson(component.getAccessor().invoke(obj)));
				} catch (ReflectiveOperationException e) {
					throw new IllegalArgumentException("record component not readable: "+component.getName(), e);
				}
			return result;
		}
		else
			throw new IllegalArgumentException("not a JSON object: "+obj.getClass().getName()+" (only Document, JsonObject, Map or record)");
	}

	// List, Collection, array or JsonArray (any implementation) -> new List
	static List<Object> toList(Object obj) {
		Objects.requireNonNull(obj, "obj");

		List<Object> result = new ArrayList<>();
		if (obj instanceof JsonArrayImpl impl)
			return impl.list;
		else if (obj instanceof JsonArray other)
			return parseArray(other.encode());
		else if (obj instanceof Collection<?> collection)
			for (Object value : collection)
				result.add(toJsonBson(value));
		else if (obj.getClass().isArray())
			for (int i=0; i<Array.getLength(obj); i++)
				result.add(toJsonBson(Array.get(obj, i)));
		else
			throw new IllegalArgumentException("not a JSON array: "+obj.getClass().getName());

		return result;
	}

	static Document parseObject(String json) {
		try {
			return Document.parse(json);
		} catch (JsonParseException | IllegalArgumentException | org.bson.BsonInvalidOperationException e) {
			throw new IllegalArgumentException("invalid JSON object: "+e.getMessage(), e);
		}
	}

	// BSON has no top-level arrays: parsed as value of a wrapping document
	@SuppressWarnings("unchecked")
	static List<Object> parseArray(String json) {
		Object result;
		try {
			result = Document.parse("{\"a\":"+json+"}").get("a");
		} catch (JsonParseException | IllegalArgumentException | org.bson.BsonInvalidOperationException e) {
			throw new IllegalArgumentException("invalid JSON array: "+e.getMessage(), e);
		}
		if (!(result instanceof List))
			throw new IllegalArgumentException("not a JSON array: "+json);
		return (List<Object>)result;
	}

	// BSON has no top-level arrays: encoded as value of a wrapping document, then cut out
	static String encodeArray(List<Object> list, boolean pretty) {
		String json = pretty ? new Document("a", list).toJson(PRETTY) : new Document("a", list).toJson();
		String result = json.substring(json.indexOf(':')+1, json.lastIndexOf('}')).strip();

		return pretty ? result.replace("\n  ", "\n") : result; // one level less indented
	}

	// deep copy of nested values (mergeIn must not share subtrees)
	static Object copy(Object value) {
		if (value instanceof Document document) {
			Document result = new Document();
			for (Map.Entry<String, Object> entry : document.entrySet())
				result.put(entry.getKey(), copy(entry.getValue()));
			return result;
		}
		else if (value instanceof List<?> list) {
			List<Object> result = new ArrayList<>();
			for (Object element : list)
				result.add(copy(element));
			return result;
		}
		else
			return value;
	}

	static String asString(Object value) {
		if (value==null)
			return null;
		if (value instanceof Document || value instanceof List)
			throw new ClassCastException("not a simple value: "+value.getClass().getSimpleName());
		return value.toString();
	}

	static Number asNumber(Object value) {
		if (value==null)
			return null;
		if (!(value instanceof Number))
			throw new ClassCastException("not a number: "+value.getClass().getSimpleName());
		return (Number)value;
	}

	static Boolean asBoolean(Object value) {
		if (value==null)
			return null;
		if (!(value instanceof Boolean))
			throw new ClassCastException("not a boolean: "+value.getClass().getSimpleName());
		return (Boolean)value;
	}

	static JsonObject asJsonObject(Object value) {
		if (value==null)
			return null;
		if (!(value instanceof Document document))
			throw new ClassCastException("not a JSON object: "+value.getClass().getSimpleName());
		return new JsonObjectImpl(document);
	}

	@SuppressWarnings("unchecked")
	static JsonArray asJsonArray(Object value) {
		if (value==null)
			return null;
		if (!(value instanceof List))
			throw new ClassCastException("not a JSON array: "+value.getClass().getSimpleName());
		return new JsonArrayImpl((List<Object>)value, true);
	}

	// equality of two BSON values: numbers by value (1 == 1L), nested values deep
	static boolean same(Object a, Object b) {
		if (a instanceof Document x && b instanceof Document y) {
			if (!x.keySet().equals(y.keySet()))
				return false;
			for (String key : x.keySet())
				if (!same(x.get(key), y.get(key)))
					return false;
			return true;
		}
		else if (a instanceof List<?> x && b instanceof List<?> y) {
			if (x.size()!=y.size())
				return false;
			for (int i=0; i<x.size(); i++)
				if (!same(x.get(i), y.get(i)))
					return false;
			return true;
		}
		else if (a instanceof Number x && b instanceof Number y) {
			BigDecimal dx = decimal(x), dy = decimal(y);
			return dx!=null && dy!=null ? dx.compareTo(dy)==0 : Objects.equals(a, b);
		}
		else
			return Objects.equals(a, b);
	}

	// null for NaN and infinity
	static BigDecimal decimal(Number number) {
		try {
			return new BigDecimal(number.toString());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	// hash code consistent with same
	static int hash(Object value) {
		if (value==null)
			return 0;
		else if (value instanceof Document document) {
			int result = 0;
			for (Map.Entry<String, Object> entry : document.entrySet())
				result += entry.getKey().hashCode() ^ hash(entry.getValue());
			return result;
		}
		else if (value instanceof List<?> list) {
			int result = 1;
			for (Object element : list)
				result = 31*result + hash(element);
			return result;
		}
		else if (value instanceof Number number) {
			BigDecimal decimal = decimal(number);
			return decimal!=null ? decimal.stripTrailingZeros().hashCode() : number.hashCode();
		}
		else
			return value.hashCode();
	}
}
