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
package io.actor4j.dataformat.spi;

import io.actor4j.core.json.JsonObject;
import io.actor4j.core.serializer.api.SerializerService;
import io.actor4j.core.utils.GenericType;
import io.actor4j.dataformat.bson.JsonObjectImpl;
import io.actor4j.dataformat.bson.utils.BsonUtils;

public class SerializerProvider implements SerializerService {
	@Override
	public byte[] encode(Object value) {
		if (value instanceof JsonObject)
			return BsonUtils.encode(((JsonObjectImpl)value).getDocument());
		else
			throw new UnsupportedOperationException();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T decode(byte[] src) {
		return (T)new JsonObjectImpl(BsonUtils.decode(src));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T decode(byte[] src, Class<T> type) {
		return (T)decode(src);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T decode(byte[] src, GenericType<T> type) {
		return (T)decode(src);
	}
}
