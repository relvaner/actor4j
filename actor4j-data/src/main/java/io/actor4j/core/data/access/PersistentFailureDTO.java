/*
 * Copyright (c) 2015-2018, David A. Bauer. All rights reserved.
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

public record PersistentFailureDTO<K, V>(PersistentDataAccessDTO<K, V> dto, int tag, Throwable throwable) {
	public static <K, V> PersistentFailureDTO<K, V> of(PersistentDataAccessDTO<K, V> dto, int tag, Throwable throwable) {
		return new PersistentFailureDTO<K, V>(dto, tag, throwable);
	}
}
