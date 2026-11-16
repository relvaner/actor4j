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

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.WriteModel;

import io.actor4j.core.utils.Pair;

// buffers write requests and writes them as one bulk write when size is reached, on flush or on close
public interface MongoBufferedBulkWriter extends AutoCloseable {
	public void write(WriteModel<Document> request, UUID id);
	public void flush();

	@Override
	public default void close() {
		flush();
	}

	// not thread-safe, see ConcurrentMongoBufferedBulkWriter
	public static MongoBufferedBulkWriter create(MongoClient client, String databaseName, String collectionName, boolean ordered, int size,
			Consumer<List<Pair<UUID, WriteModel<Document>>>> onSuccess, BiConsumer<List<Pair<UUID, WriteModel<Document>>>, Throwable> onError) {
		MongoCollection<Document> collection = client.getDatabase(databaseName).getCollection(collectionName);

		return new MongoBufferedBulkWriterImpl(collection, ordered, size, onSuccess, onError);
	}
}
