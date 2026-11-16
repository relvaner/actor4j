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

import io.actor4j.core.json.JsonArray;
import io.actor4j.core.json.JsonObject;
import io.vertx.core.json.DecodeException;

// conversion between vert.x and actor4j, contract as json-jackson:
// null and missing values are null, wrong types are a ClassCastException, invalid JSON an IllegalArgumentException
final class JsonVertxConverter {
	private JsonVertxConverter() {
	}

	// vert.x value -> actor4j (nested values wrapped)
	static Object toJava(Object value) {
		if (value instanceof io.vertx.core.json.JsonObject object)
			return new JsonObjectImpl(object);
		else if (value instanceof io.vertx.core.json.JsonArray array)
			return new JsonArrayImpl(array);
		else
			return value;
	}

	// actor4j value (any implementation) -> vert.x
	static Object toJsonVertx(Object value) {
		if (value instanceof JsonObjectImpl impl)
			return impl.jsonObject;
		else if (value instanceof JsonArrayImpl impl)
			return impl.jsonArray;
		else if (value instanceof JsonObject other)
			return parseObject(other.encode());
		else if (value instanceof JsonArray other)
			return parseArray(other.encode());
		else
			return value;
	}

	// deep copy of nested values (mergeIn must not share subtrees)
	static Object copy(Object value) {
		if (value instanceof io.vertx.core.json.JsonObject object)
			return object.copy();
		else if (value instanceof io.vertx.core.json.JsonArray array)
			return array.copy();
		else
			return value;
	}

	static io.vertx.core.json.JsonObject parseObject(String json) {
		try {
			return new io.vertx.core.json.JsonObject(json);
		} catch (DecodeException e) {
			throw new IllegalArgumentException("invalid JSON object: "+e.getMessage(), e);
		}
	}

	static io.vertx.core.json.JsonArray parseArray(String json) {
		try {
			return new io.vertx.core.json.JsonArray(json);
		} catch (DecodeException e) {
			throw new IllegalArgumentException("invalid JSON array: "+e.getMessage(), e);
		}
	}
}
