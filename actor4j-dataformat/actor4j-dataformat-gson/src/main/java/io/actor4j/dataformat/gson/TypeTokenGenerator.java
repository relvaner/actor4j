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
package io.actor4j.dataformat.gson;

import com.google.gson.reflect.TypeToken;

import io.actor4j.core.utils.GenericType;

public final class TypeTokenGenerator {
	@SuppressWarnings("unchecked")
	public static <T> TypeToken<T> generate(GenericType<T> genericType) {
		return (TypeToken<T>)TypeToken.get(genericType.getType());
	}
}
