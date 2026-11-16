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
package io.actor4j.dataformat.gson;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.ToNumberPolicy;

import io.actor4j.core.json.ObjectMapper;
import io.actor4j.core.utils.GenericType;

// errors (invalid JSON, types gson cannot map) are an IllegalArgumentException, as in json-jackson
public class ObjectMapperImpl implements ObjectMapper {
	private static final Gson gson;

	static {
		gson = JavaTimeAdapters.register(new GsonBuilder())
			.setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
			.create();
	}

	@Override
	public String mapFrom(Object obj) {
		try {
			return gson.toJson(obj);
		} catch (JsonParseException e) {
			throw new IllegalArgumentException("cannot map to JSON: "+e.getMessage(), e);
		}
	}

	@Override
	public <T> T mapTo(String json, Class<T> type) {
		return fromJson(json, type);
	}

	@Override
	public <T> T mapTo(String json, GenericType<T> type) {
		return fromJson(json, TypeTokenGenerator.generate(type).getType());
	}

	protected static <T> T fromJson(String json, Type type) {
		// gson returns null for an empty document, jackson throws
		if (json==null || json.isBlank())
			throw new IllegalArgumentException("cannot map from JSON: no content");

		try {
			return gson.fromJson(json, type);
		} catch (JsonParseException e) {
			throw new IllegalArgumentException("cannot map from JSON: "+e.getMessage(), e);
		}
	}
}
