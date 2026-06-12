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
package io.actor4j.polyglot.api;

import org.graalvm.polyglot.Value;

import io.actor4j.core.json.JsonArray;
import io.actor4j.core.json.JsonObject;

public class ValueMapper {
	protected static JsonObject toJsonObject(Value value) {
		JsonObject result = null;

		if (value == null || value.isNull() || !value.hasMembers())
			result = JsonObject.empty();
		else {
			result = JsonObject.create();
			for (String key : value.getMemberKeys()) {
				Value member = value.getMember(key);
				result.put(key, convertValue(member));
			}
		}

		return result;
	}

	protected static JsonArray toJsonArray(Value value) {
		JsonArray result = null;

		if (value == null || value.isNull() || !value.hasArrayElements())
			result = JsonArray.empty();
		else {
			result = JsonArray.create();
			long size = value.getArraySize();
			for (long i = 0; i < size; i++) {
				Value element = value.getArrayElement(i);
				result.add(convertValue(element));
			}
		}

		return result;
	}

	public static Object convertValue(Value value) {
		Object result = null;

		if (value != null && !value.isNull()) {
			if (value.hasArrayElements())
				result = toJsonArray(value);
			else if (value.hasMembers())
				result = toJsonObject(value);
			else
				result = mapPrimitiveValue(value);
		}

		return result;
	}

	protected static Object mapPrimitiveValue(Value value) {
		Object result = null;

		if (value.isString())
			result = value.asString();
		else if (value.isBoolean())
			result = value.asBoolean();
		else if (value.isNumber()) {
			if (value.fitsInInt())
				result = value.asInt();
			else if (value.fitsInLong())
				result = value.asLong();
			else
				result = value.asDouble();
		} else
			result = value.as(Object.class);

		return result;
	}
}
