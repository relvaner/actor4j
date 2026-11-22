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

import static io.actor4j.core.messages.ActorReservedTag.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.cfg.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.actor4j.core.utils.Pair;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;

// regression tests for the review 2026-11 (H1 to H4, M1, M2, M4, N1 to N3), HSQLDB in memory
public class JPABatchWriterTest {
	@Entity(name="Person")
	public static class Person {
		// JPA: persistent fields must not be public (the provider has to intercept the access)
		@Id
		private Long id;
		private String name;

		public Person() {
		}

		public Person(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	protected static final AtomicInteger databases = new AtomicInteger();

	protected EntityManagerFactory factory;
	protected EntityManager entityManager;

	protected final List<Long> succeeded = Collections.synchronizedList(new ArrayList<>());
	protected final List<Long> failed = Collections.synchronizedList(new ArrayList<>());

	@Before
	public void before() {
		factory = new Configuration()
			.addAnnotatedClass(Person.class)
			.setProperty("hibernate.connection.url", "jdbc:hsqldb:mem:writer"+databases.incrementAndGet())
			.setProperty("hibernate.connection.username", "sa")
			.setProperty("hibernate.connection.password", "")
			.setProperty("hibernate.hbm2ddl.auto", "create-drop")
			.buildSessionFactory();
		entityManager = factory.createEntityManager();
	}

	@After
	public void after() {
		entityManager.close();
		factory.close();
	}

	protected long count() {
		try (EntityManager other = factory.createEntityManager()) {
			return other.createQuery("select count(p) from Person p", Long.class).getSingleResult();
		}
	}

	protected static Long id(Pair<UUID, JPAWriteModel> pair) {
		return pair.b().obj() instanceof Person person ? person.getId() : (Long)pair.b().obj();
	}

	protected JPABatchWriter<Long, Person> writer(boolean ordered, int size) {
		return JPABatchWriter.create(factory, Person.class, ordered, size,
			batch -> batch.forEach(pair -> succeeded.add(id(pair))),
			(batch, t) -> batch.forEach(pair -> failed.add(id(pair))));
	}

	protected static JPAWriteModel insert(long id) {
		return JPAWriteModel.of(RESERVED_DATA_ACCESS_INSERT_ONE, new Person(id, "p"+id));
	}

	@Test
	public void test_flush_at_size() {
		try (JPABatchWriter<Long, Person> writer = writer(true, 3)) {
			writer.write(insert(1), UUID.randomUUID());
			writer.write(insert(2), UUID.randomUUID());
			assertEquals(0, count());
			writer.write(insert(3), UUID.randomUUID());

			assertEquals(3, count());
			assertEquals(List.of(1L, 2L, 3L), succeeded);
		}
	}

	// M4
	@Test
	public void test_close_flushes() {
		try (JPABatchWriter<Long, Person> writer = writer(true, 10)) {
			writer.write(insert(1), UUID.randomUUID());
		}

		assertEquals(1, count());
	}

	// N3
	@Test
	public void test_empty_flush_is_no_call() {
		try (JPABatchWriter<Long, Person> writer = writer(true, 10)) {
			writer.flush();
		}

		assertTrue(succeeded.isEmpty());
		assertTrue(failed.isEmpty());
	}

	@Test
	public void test_ordered_is_all_or_nothing() {
		JPAOperations.insertOne(new Person(5L, "x"), UUID.randomUUID(), entityManager);

		try (JPABatchWriter<Long, Person> writer = writer(true, 2)) {
			writer.write(insert(1), UUID.randomUUID());
			writer.write(insert(5), UUID.randomUUID()); // duplicate

			assertEquals(List.of(1L, 5L), failed);
			assertEquals(1, count());

			writer.write(insert(2), UUID.randomUUID());
			writer.write(insert(3), UUID.randomUUID());
			assertEquals(List.of(2L, 3L), succeeded);
		}
	}

	// M4: a missing key is no failure, it changes nothing and is reported as not found
	@Test
	public void test_missing_key_in_batch() {
		JPAOperations.insertOne(new Person(1L, "a"), UUID.randomUUID(), entityManager);
		List<Long> notFound = new ArrayList<>();

		try (JPABatchWriter<Long, Person> writer = JPABatchWriter.create(factory, Person.class, true, 4,
			batch -> batch.forEach(pair -> succeeded.add(id(pair))), (batch, t) -> batch.forEach(pair -> failed.add(id(pair))),
			batch -> batch.forEach(pair -> notFound.add(id(pair))))) {

			writer.write(insert(2), UUID.randomUUID());
			writer.write(JPAWriteModel.of(RESERVED_DATA_ACCESS_UPDATE_ONE, new Person(7L, "ghost")), UUID.randomUUID());
			writer.write(JPAWriteModel.of(RESERVED_DATA_ACCESS_DELETE_ONE, 99L), UUID.randomUUID());
			writer.write(JPAWriteModel.of(RESERVED_DATA_ACCESS_DELETE_ONE, 1L), UUID.randomUUID());
		}

		assertEquals(List.of(2L, 1L), succeeded);
		assertEquals(List.of(7L, 99L), notFound);
		assertTrue(failed.isEmpty());
		assertEquals(1, count()); // 2 inserted, 1 deleted, 7 not created
	}

	// M4, direct
	@Test
	public void test_missing_key_direct() {
		JPAOperations.insertOne(new Person(1L, "a"), UUID.randomUUID(), entityManager);

		assertFalse(JPAOperations.updateOne(new Person(7L, "ghost"), UUID.randomUUID(), entityManager));
		assertFalse(JPAOperations.replaceOne(new Person(7L, "ghost"), UUID.randomUUID(), entityManager));
		assertFalse(JPAOperations.deleteOne(99L, Person.class, UUID.randomUUID(), entityManager));
		assertTrue(JPAOperations.updateOne(new Person(1L, "b"), UUID.randomUUID(), entityManager));
		assertEquals(1, count());

		JPAOperations.upsertOne(new Person(7L, "set"), UUID.randomUUID(), entityManager); // SET creates it
		assertTrue(JPAOperations.deleteOne(7L, Person.class, UUID.randomUUID(), entityManager));
		assertEquals(1, count());
	}

	// H3
	@Test
	public void test_unordered_reports_each_request() {
		JPAOperations.insertOne(new Person(2L, "x"), UUID.randomUUID(), entityManager);
		entityManager.clear();

		try (JPABatchWriter<Long, Person> writer = writer(false, 3)) {
			writer.write(insert(1), UUID.randomUUID());
			writer.write(insert(2), UUID.randomUUID());
			writer.write(insert(3), UUID.randomUUID());

			assertEquals(List.of(1L, 3L), succeeded);
			assertEquals(List.of(2L), failed);
			assertEquals(3, count());
		}
	}

	// H4
	@Test
	public void test_throwing_onError_clears_the_buffer() {
		try (JPABatchWriter<Long, Person> writer = JPABatchWriter.create(factory, Person.class, true, 2,
			batch -> batch.forEach(pair -> succeeded.add(id(pair))), (batch, t) -> { throw new NullPointerException("in onError"); })) {

			writer.write(insert(1), UUID.randomUUID());
			assertThrows(NullPointerException.class, () -> writer.write(insert(1), UUID.randomUUID())); // duplicate
			writer.write(insert(3), UUID.randomUUID());
			writer.write(insert(4), UUID.randomUUID()); // automatic flush still works

			assertEquals(List.of(3L, 4L), succeeded);
			assertEquals(2, count());
		}
	}

	// M1
	@Test
	public void test_throwing_onSuccess_after_commit() {
		try (JPABatchWriter<Long, Person> writer = JPABatchWriter.create(factory, Person.class, true, 1,
			batch -> { throw new IllegalStateException("in onSuccess"); }, (batch, t) -> failed.add(id(batch.get(0))))) {

			assertThrows(IllegalStateException.class, () -> writer.write(insert(1), UUID.randomUUID()));
			assertTrue(failed.isEmpty());
			assertEquals(1, count());
		}
	}

	// N1, N2
	@Test(expected=IllegalArgumentException.class)
	public void test_invalid_size() {
		try (JPABatchWriter<Long, Person> writer = writer(true, 0)) {
			fail();
		}
	}

	@Test
	public void test_concurrent_writes() throws InterruptedException {
		try (JPABatchWriter<Long, Person> writer = ConcurrentJPABatchWriter.create(factory, Person.class, true, 10,
			batch -> batch.forEach(pair -> succeeded.add(id(pair))), (batch, t) -> failed.add(-1L))) {

			List<Thread> threads = new ArrayList<>();
			for (int t=0; t<8; t++) {
				long offset = t*100;
				threads.add(Thread.ofPlatform().start(() -> {
					for (long i=0; i<100; i++)
						writer.write(insert(offset+i), UUID.randomUUID());
				}));
			}
			for (Thread thread : threads)
				thread.join();
		}

		assertTrue(failed.toString(), failed.isEmpty());
		assertEquals(800, succeeded.size());
		assertEquals(800, count());
	}

	// H1
	@Test
	public void test_findAll() {
		JPAOperations.insertOne(new Person(1L, "a"), UUID.randomUUID(), entityManager);
		JPAOperations.insertOne(new Person(2L, "b"), UUID.randomUUID(), entityManager);

		assertEquals(2, JPAOperations.findAll(Person.class, entityManager).size());
	}

	// H2
	@Test
	public void test_queryOne_without_result_leaves_no_transaction() {
		assertNull(JPAOperations.queryOne("select p from Person p where p.id=42", Person.class, entityManager));
		assertFalse(entityManager.getTransaction().isActive());

		JPAOperations.insertOne(new Person(1L, "a"), UUID.randomUUID(), entityManager);
		assertEquals("a", JPAOperations.queryOne("select p from Person p where p.id=:id", Map.of("id", 1L), Person.class, entityManager).getName());
	}

	// H2
	@Test
	public void test_failed_operation_leaves_no_transaction() {
		JPAOperations.insertOne(new Person(1L, "a"), UUID.randomUUID(), entityManager);
		entityManager.clear();

		assertThrows(RuntimeException.class, () -> JPAOperations.insertOne(new Person(1L, "dup"), UUID.randomUUID(), entityManager));
		assertFalse(entityManager.getTransaction().isActive());

		JPAOperations.insertOne(new Person(2L, "b"), UUID.randomUUID(), entityManager);
		assertEquals(2, count());
	}
}
