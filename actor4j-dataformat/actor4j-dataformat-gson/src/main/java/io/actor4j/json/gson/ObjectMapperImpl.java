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
package io.actor4j.json.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.ToNumberPolicy;

import io.actor4j.core.json.ObjectMapper;
import io.actor4j.core.utils.GenericType;

public class ObjectMapperImpl implements ObjectMapper {
	private static final Gson gson;
	
	static {
		gson = new GsonBuilder()
			.setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
			.create();
	}

	@Override
	public String mapFrom(Object obj) {
		return gson.toJson(obj);
	}

	@Override
	public <T> T mapTo(String json, Class<T> type) {
		T result = null;
		
		try {
			result = gson.fromJson(json, type);
		} catch (JsonSyntaxException  e) {
			e.printStackTrace();
		}
		
		return result;
	}

	public <T> T mapTo(String json, GenericType<T> type) {
		T result = null;
		
		try {
			result = gson.fromJson(json, TypeTokenGenerator.generate(type));
		} catch (JsonSyntaxException e) {
			e.printStackTrace();
		}
		
		return result;
	}
}
