/*
 * Copyright (c) 2015-2023, David A. Bauer. All rights reserved.
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.WriteModel;

public class ConcurrentMongoBufferedBulkWriterImpl extends MongoBufferedBulkWriterImpl implements ConcurrentMongoBufferedBulkWriter {
	protected final Lock lock;
	
	public ConcurrentMongoBufferedBulkWriterImpl(MongoCollection<Document> collection, boolean ordered, int size,
			BiConsumer<List<WriteModel<Document>>, Throwable> onError) {
		super(collection, ordered, size, onError);
		
		lock = new ReentrantLock();
	}

	@Override
	public void write(WriteModel<Document> request) {
		lock.lock();
		try {
			super.write(request);
		}
		finally {
			lock.unlock();
		}
	}
	
	@Override
	public void flush() {
		lock.lock();
		try {
			super.flush();
		}
		finally {
			lock.unlock();
		}
	}
}
