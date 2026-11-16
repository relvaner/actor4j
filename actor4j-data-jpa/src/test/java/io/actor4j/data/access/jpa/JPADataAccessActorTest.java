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
package io.actor4j.data.access.jpa;

import static io.actor4j.core.logging.ActorLogger.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.actor4j.core.ActorRuntime;
import io.actor4j.core.ActorSystem;
import io.actor4j.core.actors.Actor;
import io.actor4j.core.actors.ActorWithCache;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.data.access.DataAccessActor;
import io.actor4j.data.access.PersistentDTO;
import io.actor4j.data.access.PersistentDataAccessDTO;
import io.actor4j.data.access.PersistentFailureDTO;
import io.actor4j.data.access.PersistentSuccessDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

// regression tests for the review 2026-11 (H2 to H4, M1, M2, N2), HSQLDB in memory (persistence unit "test")
public class JPADataAccessActorTest {
	protected ActorSystem system;
	protected ActorId client;
	protected final BlockingQueue<String> replies = new LinkedBlockingQueue<>();

	@BeforeClass
	public static void beforeClass() {
		systemLogger().setLevel(ERROR);
		logger().setLevel(ERROR);
	}

	@Before
	public void before() {
		system = ActorSystem.create(ActorRuntime.factory());
		client = system.addActor(() -> new Actor("client") {
			@Override
			public void receive(ActorMessage<?> message) {
				String key = "";
				if (message.value() instanceof PersistentSuccessDTO<?, ?> success)
					key = String.valueOf(success.dto().key());
				else if (message.value() instanceof PersistentFailureDTO<?, ?> failure)
					key = String.valueOf(failure.dto().key());
				else if (message.value() instanceof PersistentDataAccessDTO<?, ?> dto)
					key = String.valueOf(dto.key());
				replies.add(tag(message.tag())+"("+key+")");
			}
		});
		system.start();
	}

	@After
	public void after() {
		system.shutdownWithActors(true);
	}

	protected static String tag(int tag) {
		if (tag==DataAccessActor.SUCCESS) return "SUCCESS";
		if (tag==DataAccessActor.FAILURE) return "FAILURE";
		if (tag==DataAccessActor.FIND_ONE) return "FIND_ONE";
		if (tag==DataAccessActor.FIND_NONE) return "FIND_NONE";
		if (tag==DataAccessActor.FIND_ALL) return "FIND_ALL";
		return String.valueOf(tag);
	}

	protected ActorId dataAccess(boolean batch, int size) {
		return system.addActor(() -> new JPADataAccessActor<Long, Person>("data-access", "test", batch, true, size, Person.class));
	}

	protected PersistentDataAccessDTO<Long, Person> write(long id) {
		return PersistentDTO.create(id, new Person(id, "p"+id), null, client, false);
	}

	protected void send(Object value, int tag, ActorId dest) {
		system.send(ActorMessage.create(value, tag, client, dest));
	}

	protected List<String> await(int count) throws InterruptedException {
		List<String> result = new ArrayList<>();
		for (int i=0; i<count; i++) {
			String reply = replies.poll(5, TimeUnit.SECONDS);
			if (reply==null)
				break;
			result.add(reply);
		}

		return result;
	}

	protected List<String> rest() throws InterruptedException {
		Thread.sleep(300);
		List<String> result = new ArrayList<>();
		replies.drainTo(result);

		return result;
	}

	// a separate factory on the same in-memory database, without dropping the schema
	protected long count() {
		EntityManagerFactory factory = Persistence.createEntityManagerFactory("test", java.util.Map.of("jakarta.persistence.schema-generation.database.action", "none"));
		try (EntityManager entityManager = factory.createEntityManager()) {
			return entityManager.createQuery("select count(p) from Person p", Long.class).getSingleResult();
		}
		finally {
			factory.close();
		}
	}

	// M1
	@Test
	public void test_entity_name_from_the_context() throws InterruptedException {
		ActorId dataAccess = dataAccess(false, 0);
		send(write(1), DataAccessActor.INSERT_ONE, dataAccess);
		assertEquals(List.of("SUCCESS(1)"), await(1));

		send(PersistentDTO.create(1L, JPAPersistentContext.of("Person"), client), ActorWithCache.GET, dataAccess);
		assertEquals(List.of("FIND_ONE(1)"), await(1));
	}

	// M2 (jpa-sync), N2
	@Test
	public void test_findAll_and_queryOne_without_additional_success() throws InterruptedException {
		ActorId dataAccess = dataAccess(false, 0);
		send(write(1), DataAccessActor.INSERT_ONE, dataAccess);
		assertEquals(List.of("SUCCESS(1)"), await(1));

		send(PersistentDTO.create(null, client), DataAccessActor.FIND_ALL, dataAccess);
		send(PersistentDTO.create(null, JPAPersistentContext.of("select p from Person p where p.id=42", "Person"), client), DataAccessActor.QUERY_ONE, dataAccess);
		assertEquals(List.of("FIND_ALL(null)", "FIND_NONE(null)"), await(2));
		assertEquals(List.of(), rest());
	}

	// H2
	@Test
	public void test_failure_is_answered_in_batch_mode() throws InterruptedException {
		ActorId dataAccess = dataAccess(true, 10);
		send(PersistentDTO.create(1L, JPAPersistentContext.of("Unknown"), client), ActorWithCache.GET, dataAccess);

		assertEquals(List.of("FAILURE(1)"), await(1));
	}

	// H3
	@Test
	public void test_same_dto_twice_gets_two_replies() throws InterruptedException {
		ActorId dataAccess = dataAccess(true, 2);
		PersistentDataAccessDTO<Long, Person> dto = write(1);
		send(dto, ActorWithCache.SET, dataAccess);
		send(dto, ActorWithCache.SET, dataAccess);

		assertEquals(List.of("SUCCESS(1)", "SUCCESS(1)"), await(2));
		assertEquals(1, count());
	}

	// H4 (close), FLUSH without context
	@Test
	public void test_buffer_is_written_on_stop_and_on_flush() throws InterruptedException {
		ActorId dataAccess = dataAccess(true, 10);
		send(write(1), DataAccessActor.INSERT_ONE, dataAccess);
		send(PersistentDTO.create(null, client), DataAccessActor.FLUSH, dataAccess);
		assertEquals(List.of("SUCCESS(1)"), await(1));

		send(write(2), DataAccessActor.INSERT_ONE, dataAccess);
		send(write(3), DataAccessActor.INSERT_ONE, dataAccess);
		assertEquals(List.of(), rest());
		send(null, Actor.STOP, dataAccess); // the client keeps running and gets the replies

		assertEquals(List.of("SUCCESS(2)", "SUCCESS(3)"), await(2));
		assertEquals(3, count());
	}
}
