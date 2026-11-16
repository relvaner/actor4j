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
package io.actor4j.dataformat.bson.spi;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.bson.Document;

import io.actor4j.core.json.JsonObject;
import io.actor4j.core.serializer.api.SerializerService;
import io.actor4j.core.utils.GenericType;
import io.actor4j.dataformat.bson.JsonObjectImpl;
import io.actor4j.dataformat.bson.utils.BsonUtils;

// BSON for JSON objects: encode a JsonObject (any implementation) or a Document, decode to a JsonObject or a Document
public class SerializerProvider implements SerializerService {
	@Override
	public byte[] encode(Object value) {
		if (value instanceof JsonObjectImpl impl)
			return BsonUtils.encode(impl.getDocument());
		else if (value instanceof JsonObject other)
			return BsonUtils.encode(Document.parse(other.encode()));
		else if (value instanceof Document document)
			return BsonUtils.encode(document);
		else
			throw new IllegalArgumentException("BSON encodes only JSON objects: "+(value!=null ? value.getClass().getName() : null));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T decode(byte[] src) {
		return (T)new JsonObjectImpl(BsonUtils.decode(src));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T decode(byte[] src, Class<T> type) {
		if (type.isAssignableFrom(JsonObjectImpl.class))
			return (T)decode(src);
		else if (type.isAssignableFrom(Document.class))
			return (T)BsonUtils.decode(src);
		else
			throw new IllegalArgumentException("BSON decodes only to JsonObject or Document, not "+type.getName());
	}

	@Override
	public <T> T decode(byte[] src, GenericType<T> type) {
		Type raw = type.getType() instanceof ParameterizedType parameterized ? parameterized.getRawType() : type.getType();
		if (!(raw instanceof Class<?> clazz))
			throw new IllegalArgumentException("BSON decodes only to JsonObject or Document, not "+type.getType());

		@SuppressWarnings("unchecked")
		T result = (T)decode(src, clazz);
		return result;
	}
}
