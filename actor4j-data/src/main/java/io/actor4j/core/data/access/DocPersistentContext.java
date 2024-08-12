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
package io.actor4j.core.data.access;

import io.actor4j.core.json.JsonObject;

public record DocPersistentContext<K>(String keyName, JsonObject filter, JsonObject update, String collectionName) implements PersistentContext {
	public DocPersistentContext(String keyName, String collectionName) {
		this(keyName, null, null, collectionName);
	}
	
	public DocPersistentContext(String keyname, JsonObject update, String collectionName) {
		this(keyname, null, update, collectionName);
	}
	
	public static <K> DocPersistentContext<K> of(JsonObject filter, JsonObject update, String collectionName) {
		return new DocPersistentContext<K>(null, filter, update, collectionName);
	}
	
	public static <K> DocPersistentContext<K> of(String keyname, String collectionName) {
		return new DocPersistentContext<K>(keyname, null, null, collectionName);
	}
	
	public static <K> DocPersistentContext<K> of(String keyname, JsonObject update, String collectionName) {
		return new DocPersistentContext<K>(keyname, null, update, collectionName);
	}
	
	public static <K> DocPersistentContext<K> of(String collectionName) {
		return new DocPersistentContext<K>(null, collectionName);
	}
}
