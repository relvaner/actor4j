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
package io.actor4j.database.jpa;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.actor4j.core.utils.Pair;
import jakarta.persistence.EntityManager;

public interface ConcurrentJPABatchWriter<K, E> extends JPABatchWriter<K, E> {
	public static <K, E> ConcurrentJPABatchWriter<K, E> create(EntityManager entityManager, Class<E> entityType, boolean ordered, int size,
		Consumer<List<Pair<UUID, JPAWriteModel>>> onSuccess, BiConsumer<List<Pair<UUID, JPAWriteModel>>, Throwable> onError) {
		ConcurrentJPABatchWriter<K, E> result = null;
		
		try {
			result = new ConcurrentJPABatchWriterImpl<K, E>(entityManager, entityType, ordered, size, onSuccess, onError);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
}
