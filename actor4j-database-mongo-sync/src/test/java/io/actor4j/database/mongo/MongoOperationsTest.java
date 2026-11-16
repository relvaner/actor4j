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
package io.actor4j.database.mongo;

import static io.actor4j.database.mongo.MongoOperations.*;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.Filters;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import io.actor4j.core.utils.GenericType;

// regression tests for the review 2026-11 (M5, N4, N5)
public class MongoOperationsTest {
	public record Event(String name, LocalDateTime at, long counter) {
	}

	public record Stored(String _id, String name, Instant when, BigDecimal amount, long big) {
	}

	protected MongoServer server;
	protected MongoClient client;

	@Before
	public void before() {
		server = new MongoServer(new MemoryBackend());
		InetSocketAddress address = server.bind();
		client = MongoClients.create("mongodb://"+address.getHostString()+":"+address.getPort());
	}

	@After
	public void after() {
		client.close();
		server.shutdown();
	}

	@Test
	public void test_roundtrip_entity() {
		Event event = new Event("e", LocalDateTime.of(2026, 11, 16, 10, 15, 30), 5L);
		insertOne(convertToDocument(event), client, "test", "ops");

		assertEquals(event, convertToEntity(findOne(Filters.eq("name", "e"), client, "test", "ops"), Event.class));
		assertEquals(List.of(event), convertToEntities(findAll(null, client, "test", "ops"), Event.class));
	}

	// M5
	@Test
	public void test_bson_types_as_plain_json() {
		ObjectId id = new ObjectId();
		Document document = new Document("_id", id).append("name", "x").append("when", new Date(0))
			.append("amount", new Decimal128(new BigDecimal("12.50"))).append("big", 1L<<40);
		insertOne(document, client, "test", "ops");

		Stored stored = convertToEntity(findOne(Filters.eq("_id", id), client, "test", "ops"), Stored.class);
		assertEquals(id.toHexString(), stored._id());
		assertEquals(Instant.EPOCH, stored.when());
		assertEquals(0, new BigDecimal("12.50").compareTo(stored.amount()));
		assertEquals(1L<<40, stored.big());

		Map<String, Object> map = convertToEntity(document, new GenericType<Map<String, Object>>() {});
		assertEquals(id.toHexString(), map.get("_id"));
		assertEquals("1970-01-01T00:00:00Z", map.get("when"));
	}

	// N5
	@Test
	public void test_hasOne() {
		insertOne(new Document("k", 1), client, "test", "ops");

		assertTrue(hasOne(Filters.eq("k", 1), client, "test", "ops"));
		assertFalse(hasOne(Filters.eq("k", 2), client, "test", "ops"));
	}

	// N4
	@Test
	public void test_null_documents() {
		assertNull(convertToEntity(null, Event.class));
		assertEquals(List.of(), convertToEntities(null, Event.class));
		assertEquals(Arrays.asList((Event)null), convertToEntities(Arrays.asList((Document)null), Event.class));
	}
}
