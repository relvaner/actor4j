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

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mongodb.MongoBulkWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.WriteModel;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import io.actor4j.core.utils.Pair;

// regression tests for the review 2026-11 (H1, M1 to M3, M6, N1, N2, N6)
public class MongoBufferedBulkWriterTest {
	protected MongoServer server;
	protected MongoClient client;
	protected MongoCollection<Document> collection;

	protected final List<Integer> succeeded = Collections.synchronizedList(new ArrayList<>());
	protected final List<Integer> failed = Collections.synchronizedList(new ArrayList<>());
	protected final List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

	@Before
	public void before() {
		server = new MongoServer(new MemoryBackend());
		InetSocketAddress address = server.bind();
		client = MongoClients.create("mongodb://"+address.getHostString()+":"+address.getPort());
		collection = client.getDatabase("test").getCollection("writer");
	}

	@After
	public void after() {
		client.close();
		server.shutdown();
	}

	protected static WriteModel<Document> insert(int id) {
		return new InsertOneModel<>(new Document("_id", id));
	}

	protected static int id(Pair<UUID, WriteModel<Document>> pair) {
		return ((InsertOneModel<Document>)pair.b()).getDocument().getInteger("_id");
	}

	protected MongoBufferedBulkWriter writer(boolean ordered, int size) {
		return MongoBufferedBulkWriter.create(client, "test", "writer", ordered, size,
			batch -> batch.forEach(pair -> succeeded.add(id(pair))),
			(batch, t) -> { batch.forEach(pair -> failed.add(id(pair))); errors.add(t); });
	}

	@Test
	public void test_flush_at_size() {
		try (MongoBufferedBulkWriter writer = writer(true, 3)) {
			writer.write(insert(1), UUID.randomUUID());
			writer.write(insert(2), UUID.randomUUID());
			assertEquals(0, collection.countDocuments());
			writer.write(insert(3), UUID.randomUUID());
			
			assertEquals(3, collection.countDocuments());
			assertEquals(List.of(1, 2, 3), succeeded);
		}
	}
	
	@Test
	public void test_close_flushes() {
		try (MongoBufferedBulkWriter writer = writer(true, 10)) {
			writer.write(insert(1), UUID.randomUUID());
		}
		
		assertEquals(1, collection.countDocuments());
	}
	
	// M1
	@Test
	public void test_empty_flush_is_no_error() {
		try (MongoBufferedBulkWriter writer = writer(true, 10)) {
			writer.flush();
		}
		
		assertTrue(succeeded.isEmpty());
		assertTrue(failed.isEmpty());
	}
	
	// H1
	@Test
	public void test_throwing_onError_clears_the_buffer() {
		collection.insertOne(new Document("_id", 1));
		try (MongoBufferedBulkWriter writer = MongoBufferedBulkWriter.create(client, "test", "writer", true, 2,
			batch -> batch.forEach(pair -> succeeded.add(id(pair))), (batch, t) -> { throw new NullPointerException("in onError"); })) {
			
			writer.write(insert(1), UUID.randomUUID());
			assertThrows(NullPointerException.class, () -> writer.write(insert(2), UUID.randomUUID()));
			writer.write(insert(3), UUID.randomUUID());
			writer.write(insert(4), UUID.randomUUID()); // automatic flush still works
			
			assertEquals(List.of(3, 4), succeeded);
			assertEquals(3, collection.countDocuments());
		}
	}
	
	// M2
	@Test
	public void test_throwing_onSuccess_is_not_an_error() {
		try (MongoBufferedBulkWriter writer = MongoBufferedBulkWriter.create(client, "test", "writer", true, 1,
			batch -> { throw new IllegalStateException("in onSuccess"); }, (batch, t) -> failed.add(id(batch.get(0))))) {
			
			assertThrows(IllegalStateException.class, () -> writer.write(insert(1), UUID.randomUUID()));
			assertTrue(failed.isEmpty());
			assertEquals(1, collection.countDocuments());
		}
	}
	
	// M3
	@Test
	public void test_partial_success_ordered() {
		collection.insertOne(new Document("_id", 2));
		try (MongoBufferedBulkWriter writer = writer(true, 3)) {
			writer.write(insert(1), UUID.randomUUID());
			writer.write(insert(2), UUID.randomUUID());
			writer.write(insert(3), UUID.randomUUID());
			
			assertEquals(List.of(1), succeeded);
			assertEquals(List.of(2, 3), failed);
			assertTrue(errors.get(0) instanceof MongoBulkWriteException);
		}
	}
	
	@Test
	public void test_partial_success_unordered() {
		collection.insertOne(new Document("_id", 2));
		try (MongoBufferedBulkWriter writer = writer(false, 3)) {
			writer.write(insert(1), UUID.randomUUID());
			writer.write(insert(2), UUID.randomUUID());
			writer.write(insert(3), UUID.randomUUID());
			
			assertEquals(List.of(1, 3), succeeded);
			assertEquals(List.of(2), failed);
			assertEquals(3, collection.countDocuments());
		}
	}
	
	// N1, N2
	@Test(expected=IllegalArgumentException.class)
	public void test_invalid_size() {
		try (MongoBufferedBulkWriter writer = writer(true, 0)) {
			fail();
		}
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void test_invalid_collection_name() {
		try (MongoBufferedBulkWriter writer = MongoBufferedBulkWriter.create(client, "test", "", true, 10, null, null)) {
			fail();
		}
	}
	
	@Test
	public void test_concurrent_writes() throws InterruptedException {
		try (MongoBufferedBulkWriter writer = ConcurrentMongoBufferedBulkWriter.create(client, "test", "writer", true, 10,
			batch -> batch.forEach(pair -> succeeded.add(id(pair))), (batch, t) -> errors.add(t))) {
			
			List<Thread> threads = new ArrayList<>();
			for (int t=0; t<8; t++) {
				int offset = t*100;
				threads.add(Thread.ofPlatform().start(() -> {
					for (int i=0; i<100; i++)
						writer.write(insert(offset+i), UUID.randomUUID());
				}));
			}
			for (Thread thread : threads)
				thread.join();
		}
		
		assertTrue(errors.toString(), errors.isEmpty());
		assertEquals(800, succeeded.size());
		assertEquals(800, collection.countDocuments());
	}
	
	// M6
	@Test
	public void test_concurrent_write_is_not_blocked_by_a_bulk_write() throws InterruptedException {
		AtomicInteger calls = new AtomicInteger();
		@SuppressWarnings("unchecked")
		MongoCollection<Document> slow = (MongoCollection<Document>)Proxy.newProxyInstance(MongoCollection.class.getClassLoader(), new Class<?>[] {MongoCollection.class}, (p, m, a) -> {
			if (m.getName().equals("bulkWrite")) {
				calls.incrementAndGet();
				Thread.sleep(500);
				return null;
			}
			throw new UnsupportedOperationException(m.getName());
		});
		try (ConcurrentMongoBufferedBulkWriterImpl writer = new ConcurrentMongoBufferedBulkWriterImpl(slow, true, 100, null, null)) {
			writer.write(insert(0), UUID.randomUUID());
			Thread flusher = Thread.ofPlatform().start(writer::flush);
			Thread.sleep(100);
			
			long start = System.nanoTime();
			writer.write(insert(1), UUID.randomUUID());
			long blocked = (System.nanoTime()-start)/1_000_000;
			flusher.join();
			
			assertTrue("blocked "+blocked+" ms", blocked<100);
			assertEquals(1, calls.get());
		}
	}
}
