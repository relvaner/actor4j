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

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.WriteModel;

public class MongoBufferedBulkWriterImpl implements MongoBufferedBulkWriter {
	protected final MongoCollection<Document> collection;
	
	protected final Queue<WriteModel<Document>> requestsQueue;
	protected final boolean ordered;
	
	protected final int size;
	protected final AtomicInteger counter;
	protected final Lock lock;
	
	protected final BiConsumer<List<WriteModel<Document>>, Throwable> onError;
	
	public MongoBufferedBulkWriterImpl(MongoCollection<Document> collection, boolean ordered, int size, 
			BiConsumer<List<WriteModel<Document>>, Throwable> onError) {
		super();
		this.collection = collection;
		this.ordered = ordered;
		this.size = size;
		this.onError = onError;
		
		counter = new AtomicInteger(0);
		requestsQueue = new ConcurrentLinkedQueue<>();
		lock = new ReentrantLock();
	}
	
	@Override
	public void write(WriteModel<Document> request) {
		lock.lock();
		try {
			requestsQueue.offer(request);
			if (counter.incrementAndGet()==size)
				flush();
		}
		finally {
			lock.unlock();
		}
	}
	
	@Override
	public void flush() {
		lock.lock();
		try {
			List<WriteModel<Document>> requests = null;
			try {
				requests = new LinkedList<>(requestsQueue);
				if (ordered)
					collection.bulkWrite(requests);
				else
					collection.bulkWrite(requests, new BulkWriteOptions().ordered(false));
				}
			catch(Exception e) {
				e.printStackTrace();
				
				if (onError!=null)
					onError.accept(requests, e);
			}
			
			requestsQueue.clear();
			counter.set(0);
		}
		finally {
			lock.unlock();
		}
	}
}
