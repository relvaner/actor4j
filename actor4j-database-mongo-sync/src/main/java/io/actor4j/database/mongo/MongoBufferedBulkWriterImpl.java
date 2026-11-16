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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.bson.Document;

import com.mongodb.MongoBulkWriteException;
import com.mongodb.bulk.BulkWriteError;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.WriteModel;

import io.actor4j.core.utils.Pair;

// not thread-safe, see ConcurrentMongoBufferedBulkWriterImpl
public class MongoBufferedBulkWriterImpl implements MongoBufferedBulkWriter {
	protected final MongoCollection<Document> collection;

	protected List<Pair<UUID, WriteModel<Document>>> requests;
	protected final boolean ordered;

	protected final int size;

	protected final Consumer<List<Pair<UUID, WriteModel<Document>>>> onSuccess;
	protected final BiConsumer<List<Pair<UUID, WriteModel<Document>>>, Throwable> onError;

	public MongoBufferedBulkWriterImpl(MongoCollection<Document> collection, boolean ordered, int size,
			Consumer<List<Pair<UUID, WriteModel<Document>>>> onSuccess, BiConsumer<List<Pair<UUID, WriteModel<Document>>>, Throwable> onError) {
		super();
		if (size<1)
			throw new IllegalArgumentException("size must be at least 1: "+size);

		this.collection = collection;
		this.ordered = ordered;
		this.size = size;
		this.onSuccess = onSuccess;
		this.onError = onError;

		requests = new ArrayList<>();
	}

	@Override
	public void write(WriteModel<Document> request, UUID id) {
		requests.add(Pair.of(id, request));
		if (requests.size()>=size)
			flush();
	}

	@Override
	public void flush() {
		List<Pair<UUID, WriteModel<Document>>> batch = take();
		if (batch!=null)
			bulkWrite(batch);
	}

	// the buffered requests (buffer is empty afterwards), null if there are none
	protected List<Pair<UUID, WriteModel<Document>>> take() {
		if (requests.isEmpty())
			return null;

		List<Pair<UUID, WriteModel<Document>>> result = requests;
		requests = new ArrayList<>();

		return result;
	}

	// exceptions of the callbacks are passed on, the buffer is already empty
	protected void bulkWrite(List<Pair<UUID, WriteModel<Document>>> batch) {
		List<WriteModel<Document>> writeModels = batch.stream().map(Pair::b).toList();

		try {
			collection.bulkWrite(writeModels, new BulkWriteOptions().ordered(ordered));
		}
		catch(MongoBulkWriteException e) {
			partial(batch, e);
			return;
		}
		catch(RuntimeException e) {
			error(batch, e);
			return;
		}

		if (onSuccess!=null)
			onSuccess.accept(batch);
	}

	// ordered: written up to the first error, unordered: all except the errors
	protected void partial(List<Pair<UUID, WriteModel<Document>>> batch, MongoBulkWriteException e) {
		Set<Integer> failedIndexes = new HashSet<>();
		for (BulkWriteError error : e.getWriteErrors())
			failedIndexes.add(error.getIndex());
		int firstFailed = failedIndexes.isEmpty() ? batch.size() : failedIndexes.stream().mapToInt(i -> i).min().getAsInt();

		List<Pair<UUID, WriteModel<Document>>> succeeded = new ArrayList<>();
		List<Pair<UUID, WriteModel<Document>>> failed = new ArrayList<>();
		for (int i=0; i<batch.size(); i++)
			if (ordered ? i<firstFailed : !failedIndexes.contains(i))
				succeeded.add(batch.get(i));
			else
				failed.add(batch.get(i));
		// e.g. a write concern error: nothing is known as failed, report all
		if (failed.isEmpty()) {
			failed = batch;
			succeeded = List.of();
		}

		try {
			if (!succeeded.isEmpty() && onSuccess!=null)
				onSuccess.accept(succeeded);
		}
		finally {
			error(failed, e);
		}
	}

	protected void error(List<Pair<UUID, WriteModel<Document>>> batch, Throwable t) {
		if (onError!=null)
			onError.accept(batch, t);
		else
			t.printStackTrace();
	}
}
