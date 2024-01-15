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
package io.actor4j.core.persistence;

import java.util.List;

import io.actor4j.core.json.JsonObject;
import io.actor4j.core.json.ObjectMapper;
import io.actor4j.core.utils.GenericType;

public record Recovery<S, E>(ActorPersistenceDTO<S> state, List<ActorPersistenceDTO<E>> events) {
	private static final ObjectMapper objectMapper;
	
	static {
		objectMapper = ObjectMapper.create();
	}
	
	@Override
	public String toString() {
		return "Recovery [state=" + state + ", events=" + events + "]";
	}
	
	public static boolean isError(JsonObject value) {		
		return value!=null ? value.containsKey("error") : false;
	}
	
	public static String getErrorMsg(JsonObject value) {
		return value!=null ? value.getString("error") : null;
	}

	public static <A, B> Recovery<A, B> convertValue(JsonObject value, GenericType<Recovery<A, B>> valueTypeRef) {
		Recovery<A, B> result = null;

		if (value!=null && valueTypeRef!=null)
			try {
				result = (Recovery<A, B>)objectMapper.mapTo(value.encode(), valueTypeRef);
			} catch (ClassCastException e) {
				e.printStackTrace();
			}

		return result;
	}
}
