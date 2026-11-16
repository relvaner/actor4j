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
package io.actor4j.data.access.mongo;

import static io.actor4j.core.logging.ActorLogger.*;
import static org.junit.Assert.*;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import io.actor4j.core.ActorRuntime;
import io.actor4j.core.ActorSystem;
import io.actor4j.core.actors.Actor;
import io.actor4j.core.actors.ActorWithCache;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.json.JsonObject;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.data.access.DataAccessActor;
import io.actor4j.data.access.DefaultDocPersistentContext;
import io.actor4j.data.access.DocPersistentContext;
import io.actor4j.data.access.PersistentDTO;
import io.actor4j.data.access.PersistentDataAccessDTO;
import io.actor4j.data.access.PersistentFailureDTO;
import io.actor4j.data.access.PersistentSuccessDTO;

// regression tests for the review 2026-11 (H1 to H3, M1, M3, N1)
public class MongoDataAccessActorTest {
	public static class Entity {
		public String key;
		public String value;

		public Entity() {
		}

		public Entity(String key, String value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public int hashCode() {
			return Objects.hash(key, value);
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof Entity other && Objects.equals(key, other.key) && Objects.equals(value, other.value);
		}
	}

	protected MongoServer server;
	protected MongoClient mongo;
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
		server = new MongoServer(new MemoryBackend());
		InetSocketAddress address = server.bind();
		mongo = MongoClients.create("mongodb://"+address.getHostString()+":"+address.getPort());

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
		mongo.close();
		server.shutdown();
	}

	protected static String tag(int tag) {
		if (tag==DataAccessActor.SUCCESS) return "SUCCESS";
		if (tag==DataAccessActor.FAILURE) return "FAILURE";
		if (tag==DataAccessActor.FIND_ONE) return "FIND_ONE";
		if (tag==DataAccessActor.FIND_NONE) return "FIND_NONE";
		if (tag==DataAccessActor.FIND_ALL) return "FIND_ALL";
		return String.valueOf(tag);
	}

	protected ActorId dataAccess(boolean bulk, int size) {
		return system.addActor(() -> new MongoDataAccessActor<String, Entity>("data-access", mongo, "db", bulk, true, size, Entity.class));
	}

	protected static DocPersistentContext ctx(String collection) {
		return DocPersistentContext.of("key", collection);
	}

	protected PersistentDataAccessDTO<String, Entity> write(String key, String collection) {
		return PersistentDTO.create(key, new Entity(key, "v-"+key), ctx(collection), client, false);
	}

	protected void send(Object value, int tag, ActorId dest) {
		system.send(ActorMessage.create(value, tag, client, dest));
	}

	protected List<String> await(int count) throws InterruptedException {
		List<String> result = new ArrayList<>();
		for (int i=0; i<count; i++) {
			String reply = replies.poll(3, TimeUnit.SECONDS);
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

	protected long count(String collection) {
		return mongo.getDatabase("db").getCollection(collection).countDocuments();
	}

	// H2
	@Test
	public void test_buffer_is_written_on_stop() throws InterruptedException {
		ActorId dataAccess = dataAccess(true, 10);
		for (String key : List.of("a", "b", "c"))
			send(write(key, "A"), DataAccessActor.INSERT_ONE, dataAccess);
		assertEquals(List.of(), rest());

		send(null, Actor.STOP, dataAccess); // the client keeps running and gets the replies
		
		assertEquals(List.of("SUCCESS(a)", "SUCCESS(b)", "SUCCESS(c)"), await(3));
		assertEquals(3, count("A"));
	}

	// H2
	@Test
	public void test_flush_without_context_writes_all_collections() throws InterruptedException {
		ActorId dataAccess = dataAccess(true, 10);
		send(write("a1", "A"), DataAccessActor.INSERT_ONE, dataAccess);
		send(write("b1", "B"), DataAccessActor.INSERT_ONE, dataAccess);
		send(PersistentDTO.create(null, client), DataAccessActor.FLUSH, dataAccess);

		List<String> result = await(2);
		assertTrue(result.toString(), result.containsAll(List.of("SUCCESS(a1)", "SUCCESS(b1)")));
		assertEquals(1, count("A"));
		assertEquals(1, count("B"));
	}

	// H1
	@Test
	public void test_failure_is_answered_in_bulk_mode() throws InterruptedException {
		ActorId dataAccess = dataAccess(true, 10);
		send(PersistentDTO.create("x", null, client), ActorWithCache.GET, dataAccess);

		assertEquals(List.of("FAILURE(x)"), await(1));
	}

	// H3
	@Test
	public void test_same_dto_twice_gets_two_replies() throws InterruptedException {
		ActorId dataAccess = dataAccess(true, 2);
		PersistentDataAccessDTO<String, Entity> dto = write("k", "S");
		send(dto, ActorWithCache.SET, dataAccess);
		send(dto, ActorWithCache.SET, dataAccess);

		assertEquals(List.of("SUCCESS(k)", "SUCCESS(k)"), await(2));
		assertEquals(1, count("S"));
	}

	// M1
	@Test
	public void test_set_twice_in_bulk_gives_one_document() throws InterruptedException {
		ActorId dataAccess = dataAccess(true, 10);
		send(write("dup", "D"), ActorWithCache.SET, dataAccess);
		send(write("dup", "D"), ActorWithCache.SET, dataAccess);
		send(PersistentDTO.create(null, ctx("D"), client), DataAccessActor.FLUSH, dataAccess);

		assertEquals(2, await(2).size());
		assertEquals(1, count("D"));
	}

	// M1, data-mongo M2 (SET of a key that is cached but missing in the database)
	@Test
	public void test_set_of_a_cached_but_missing_key_inserts_it() throws InterruptedException {
		ActorId dataAccess = dataAccess(false, 0);
		send(PersistentDTO.create("ghost", new Entity("ghost", "v"), ctx("R"), client, true), ActorWithCache.SET, dataAccess);

		assertEquals(List.of("SUCCESS(ghost)"), await(1));
		assertEquals(1, count("R"));
	}

	// M3
	@Test
	public void test_filter_of_the_context_is_not_changed() throws InterruptedException {
		ActorId dataAccess = dataAccess(false, 0);
		JsonObject filter = JsonObject.create().put("tenant", "t1");
		DocPersistentContext shared = new DefaultDocPersistentContext("key", filter, null, "F");
		send(PersistentDTO.create("x1", shared, client), ActorWithCache.GET, dataAccess);

		assertEquals(List.of("FIND_NONE(x1)"), await(1));
		assertEquals("{\"tenant\":\"t1\"}", filter.encode());
	}

	// N1
	@Test
	public void test_reads_get_no_additional_success() throws InterruptedException {
		mongo.getDatabase("db").getCollection("R").insertOne(new Document("key", "a").append("value", "v"));
		ActorId dataAccess = dataAccess(false, 0);
		send(PersistentDTO.create(null, ctx("R"), client), DataAccessActor.FIND_ALL, dataAccess);

		assertEquals(List.of("FIND_ALL(null)"), await(1));
		assertEquals(List.of(), rest());
	}
}
