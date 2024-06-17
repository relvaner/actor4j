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
package io.actor4j.dataformat.msgpack.jackson;

import org.msgpack.jackson.dataformat.MessagePackFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;

import io.actor4j.core.json.ObjectMapper;
import io.actor4j.core.utils.GenericType;

public class ObjectMapperImpl implements ObjectMapper {
	private static final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
	
	static {
		objectMapper = new com.fasterxml.jackson.databind.ObjectMapper(new MessagePackFactory());
		
		objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
	}
	
	public static com.fasterxml.jackson.databind.ObjectMapper underlyingImpl() {
		return objectMapper;
	}

	@Deprecated
	@Override
	public String mapFrom(Object obj) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T mapTo(String json, Class<T> type) {
		T result = null;
		
		try {
			result = objectMapper.readValue(json, type);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		
		return result;
	}

	@Override
	public <T> T mapTo(String json, GenericType<T> type) {
		T result = null;
		
		try {
			result = objectMapper.readValue(json, TypeReferenceGenerator.generate(type));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		
		return result;
	}
}
