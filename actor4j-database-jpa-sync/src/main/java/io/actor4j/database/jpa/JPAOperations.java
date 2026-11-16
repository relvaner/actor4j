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
package io.actor4j.database.jpa;

import static io.actor4j.core.messages.ActorReservedTag.RESERVED_CACHE_SET;
import static io.actor4j.core.messages.ActorReservedTag.RESERVED_DATA_ACCESS_DELETE_ONE;
import static io.actor4j.core.messages.ActorReservedTag.RESERVED_DATA_ACCESS_INSERT_ONE;
import static io.actor4j.core.messages.ActorReservedTag.RESERVED_DATA_ACCESS_REPLACE_ONE;
import static io.actor4j.core.messages.ActorReservedTag.RESERVED_DATA_ACCESS_UPDATE_ONE;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaQuery;

// replace, update and delete return false if the key does not exist (nothing is changed then),
// with a batch writer they return true and a missing key is reported by its onNotFound
public class JPAOperations {
	public static boolean hasOne(Object primaryKey, Class<?> entityType, EntityManager entityManager) {
		return entityManager.find(entityType, primaryKey)!=null;
	}

	// whether an entity with the id of this entity exists in the database
	public static boolean exists(Object entity, EntityManager entityManager) {
		Object primaryKey = entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);

		return primaryKey!=null && entityManager.find(entity.getClass(), primaryKey)!=null;
	}

	public static <K, E> void insertOne(E entity, UUID id, EntityManager entityManager) {
		insertOne(entity, id, entityManager, null);
	}

	public static <K, E> void upsertOne(E entity, UUID id, EntityManager entityManager) {
		upsertOne(entity, id, entityManager, null);
	}

	public static <K, E> boolean replaceOne(E entity, UUID id, EntityManager entityManager) {
		return replaceOne(entity, id, entityManager, null);
	}

	public static <K, E> boolean updateOne(E entity, UUID id, EntityManager entityManager) {
		return updateOne(entity, id, entityManager, null);
	}

	public static <K, E> boolean deleteOne(K primaryKey, Class<E> entityType, UUID id, EntityManager entityManager) {
		return deleteOne(primaryKey, entityType, id, entityManager, null);
	}

	public static <K, E> void insertOne(E entity, UUID id, EntityManager entityManager, JPABatchWriter<K, E> batchWriter) {
		if (batchWriter!=null)
			batchWriter.write(JPAWriteModel.of(RESERVED_DATA_ACCESS_INSERT_ONE, entity), id);
		else
			transaction(entityManager, () -> {
				entityManager.persist(entity);
				return true;
			});
	}

	// inserts or replaces (merge), as for SET
	public static <K, E> void upsertOne(E entity, UUID id, EntityManager entityManager, JPABatchWriter<K, E> batchWriter) {
		if (batchWriter!=null)
			batchWriter.write(JPAWriteModel.of(RESERVED_CACHE_SET, entity), id);
		else
			transaction(entityManager, () -> {
				entityManager.merge(entity);
				return true;
			});
	}

	public static <K, E> boolean replaceOne(E entity, UUID id, EntityManager entityManager, JPABatchWriter<K, E> batchWriter) {
		if (batchWriter!=null) {
			batchWriter.write(JPAWriteModel.of(RESERVED_DATA_ACCESS_REPLACE_ONE, entity), id);
			return true;
		}
		else
			return merge(entity, entityManager);
	}

	public static <K, E> boolean updateOne(E entity, UUID id, EntityManager entityManager, JPABatchWriter<K, E> batchWriter) {
		if (batchWriter!=null) {
			batchWriter.write(JPAWriteModel.of(RESERVED_DATA_ACCESS_UPDATE_ONE, entity), id);
			return true;
		}
		else
			return merge(entity, entityManager);
	}

	protected static <E> boolean merge(E entity, EntityManager entityManager) {
		return transaction(entityManager, () -> {
			if (!exists(entity, entityManager))
				return false;
			entityManager.merge(entity);
			return true;
		});
	}

	public static <K, E> boolean deleteOne(K primaryKey, Class<E> entityType, UUID id, EntityManager entityManager, JPABatchWriter<K, E> batchWriter) {
		if (batchWriter!=null) {
			batchWriter.write(JPAWriteModel.of(RESERVED_DATA_ACCESS_DELETE_ONE, primaryKey), id);
			return true;
		}
		else
			return transaction(entityManager, () -> {
				E entity = entityManager.find(entityType, primaryKey);
				if (entity==null)
					return false;
				entityManager.remove(entity);
				return true;
			});
	}

	// rollback and a cleared persistence context on failure, the transaction never stays open
	protected static <T> T transaction(EntityManager entityManager, Supplier<T> operation) {
		EntityTransaction transaction = entityManager.getTransaction();
		transaction.begin();
		try {
			T result = operation.get();
			transaction.commit();

			return result;
		}
		catch(RuntimeException e) {
			if (transaction.isActive())
				transaction.rollback();
			entityManager.clear();
			throw e;
		}
	}

	// JPQL, null if there is no result
	public static <E> E queryOne(String jpql, Class<E> entityType, EntityManager entityManager) {
		return queryOne(jpql, Map.of(), entityType, entityManager);
	}

	// JPQL with named parameters (:name), null if there is no result
	public static <E> E queryOne(String jpql, Map<String, ?> parameters, Class<E> entityType, EntityManager entityManager) {
		try {
			return query(jpql, parameters, entityType, entityManager).getSingleResult();
		}
		catch(NoResultException e) {
			return null;
		}
	}

	public static <E> List<E> queryAll(String jpql, Class<E> entityType, EntityManager entityManager) {
		return queryAll(jpql, Map.of(), entityType, entityManager);
	}

	public static <E> List<E> queryAll(String jpql, Map<String, ?> parameters, Class<E> entityType, EntityManager entityManager) {
		return query(jpql, parameters, entityType, entityManager).getResultList();
	}

	protected static <E> TypedQuery<E> query(String jpql, Map<String, ?> parameters, Class<E> entityType, EntityManager entityManager) {
		TypedQuery<E> query = entityManager.createQuery(jpql, entityType);
		parameters.forEach(query::setParameter);

		return query;
	}

	public static <E> E findOne(Object primaryKey, Class<E> entityType, EntityManager entityManager) {
		return entityManager.find(entityType, primaryKey);
	}

	public static <E> List<E> findAll(Class<E> entityType, EntityManager entityManager) {
		CriteriaQuery<E> query = entityManager.getCriteriaBuilder().createQuery(entityType);
		query.select(query.from(entityType));

		return entityManager.createQuery(query).getResultList();
	}
}
