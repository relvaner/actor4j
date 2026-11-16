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
package io.actor4j.dataformat.msgpack.jackson.utils;

import java.io.IOException;

import org.msgpack.core.MessagePackException;
import org.msgpack.jackson.dataformat.MessagePackFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.actor4j.core.utils.GenericType;
import io.actor4j.dataformat.msgpack.jackson.TypeReferenceGenerator;

// MessagePack via Jackson, errors (also invalid bytes) are an IllegalArgumentException
public final class MsgPackUtils {
	private static final ObjectMapper objectMapper;

	static {
		objectMapper = new ObjectMapper(new MessagePackFactory());
		objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
	}

	public static byte[] encode(Object obj) {
		try {
			return objectMapper.writeValueAsBytes(obj);
		} catch (JsonProcessingException | MessagePackException e) {
			throw new IllegalArgumentException("cannot encode to MessagePack: "+e.getMessage(), e);
		}
	}

	// without a type: Map, List, String, Number, Boolean or null
	public static Object decode(byte[] src) {
		return decode(src, Object.class);
	}

	public static <T> T decode(byte[] src, Class<T> type) {
		try {
			return objectMapper.readValue(src, type);
		} catch (IOException | MessagePackException e) {
			throw new IllegalArgumentException("cannot decode from MessagePack: "+e.getMessage(), e);
		}
	}

	public static <T> T decode(byte[] src, GenericType<T> type) {
		try {
			return objectMapper.readValue(src, TypeReferenceGenerator.generate(type));
		} catch (IOException | MessagePackException e) {
			throw new IllegalArgumentException("cannot decode from MessagePack: "+e.getMessage(), e);
		}
	}
}
