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

import static io.actor4j.core.messages.ActorReservedTag.RESERVED_DATA_ACCESS_DELETE_ONE;
import static io.actor4j.core.messages.ActorReservedTag.RESERVED_DATA_ACCESS_INSERT_ONE;
import static io.actor4j.core.messages.ActorReservedTag.RESERVED_DATA_ACCESS_REPLACE_ONE;
import static io.actor4j.core.messages.ActorReservedTag.RESERVED_DATA_ACCESS_UPDATE_ONE;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaQuery;

public class JPAOperations {
	public static boolean hasOne(Object primaryKey, Class<?> entityType, EntityManager entityManager) {
		return entityManager.find(entityType, primaryKey)!=null;
	}

	public static <K, E> void insertOne(E entity, UUID id, EntityManager entityManager) {
		insertOne(entity, id, entityManager, null);
	}

	public static <K, E> void replaceOne(E entity, UUID id, EntityManager entityManager) {
		replaceOne(entity, id, entityManager, null);
	}

	public static <K, E> void updateOne(E entity, UUID id, EntityManager entityManager) {
		updateOne(entity, id, entityManager, null);
	}

	public static <K, E> void deleteOne(K primaryKey, Class<E> entityType, UUID id, EntityManager entityManager) {
		deleteOne(primaryKey, entityType, id, entityManager, null);
	}

	public static <K, E> void insertOne(E entity, UUID id, EntityManager entityManager, JPABatchWriter<K, E> batchWriter) {
		if (batchWriter!=null)
			batchWriter.write(JPAWriteModel.of(RESERVED_DATA_ACCESS_INSERT_ONE, entity), id);
		else
			transaction(entityManager, () -> entityManager.persist(entity));
	}

	public static <K, E> void replaceOne(E entity, UUID id, EntityManager entityManager, JPABatchWriter<K, E> batchWriter) {
		if (batchWriter!=null)
			batchWriter.write(JPAWriteModel.of(RESERVED_DATA_ACCESS_REPLACE_ONE, entity), id);
		else
			transaction(entityManager, () -> entityManager.merge(entity));
	}

	public static <K, E> void updateOne(E entity, UUID id, EntityManager entityManager, JPABatchWriter<K, E> batchWriter) {
		if (batchWriter!=null)
			batchWriter.write(JPAWriteModel.of(RESERVED_DATA_ACCESS_UPDATE_ONE, entity), id);
		else
			transaction(entityManager, () -> entityManager.merge(entity));
	}

	public static <K, E> void deleteOne(K primaryKey, Class<E> entityType, UUID id, EntityManager entityManager, JPABatchWriter<K, E> batchWriter) {
		if (batchWriter!=null)
			batchWriter.write(JPAWriteModel.of(RESERVED_DATA_ACCESS_DELETE_ONE, primaryKey), id);
		else
			transaction(entityManager, () -> entityManager.remove(entityManager.getReference(entityType, primaryKey)));
	}

	// rollback and a cleared persistence context on failure, the transaction never stays open
	protected static void transaction(EntityManager entityManager, Runnable operation) {
		EntityTransaction transaction = entityManager.getTransaction();
		transaction.begin();
		try {
			operation.run();
			transaction.commit();
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
