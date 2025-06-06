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

import io.actor4j.core.id.ActorId;

public record PersistentSuccessDTO<K, V>(PersistentDataAccessDTO<K, V> dto, int tag) {
	public static <K, V> PersistentSuccessDTO<K, V> of(PersistentDataAccessDTO<K, V> dto, int tag) {
		return new PersistentSuccessDTO<K, V>(dto, tag);
	}
	
	public PersistentSuccessDTO<K, V> shallowCopy(ActorId source) {
		return new PersistentSuccessDTO<K, V>(dto.shallowCopy(source), tag);
	}
}
