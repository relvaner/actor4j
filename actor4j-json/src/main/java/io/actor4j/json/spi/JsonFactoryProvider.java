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
package io.actor4j.json.spi;

import java.util.List;

import io.actor4j.core.json.JsonArray;
import io.actor4j.core.json.JsonObject;
import io.actor4j.core.json.api.JsonFactoryService;
import io.actor4j.json.JsonArrayImpl;
import io.actor4j.json.JsonObjectImpl;

public class JsonFactoryProvider implements JsonFactoryService {
	@Override
	public JsonObject createJsonObject() {
		return new JsonObjectImpl();
	}

	@Override
	public JsonObject createJsonObject(Object obj) {
		return new JsonObjectImpl(obj);
	}

	@Override
	public JsonObject createJsonObject(String json) {
		return new JsonObjectImpl(json);
	}

	@Override
	public JsonArray createJsonArray() {
		return new JsonArrayImpl();
	}

	@Override
	public JsonArray createJsonArray(List<?> list) {
		return new JsonArrayImpl(list);
	}

	@Override
	public JsonArray createJsonArray(String json) {
		return new JsonArrayImpl(json);
	}
}
