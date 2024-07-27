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
	protected int count;
	
	protected final BiConsumer<List<WriteModel<Document>>, Throwable> onError;
	
	public MongoBufferedBulkWriterImpl(MongoCollection<Document> collection, boolean ordered, int size, 
			BiConsumer<List<WriteModel<Document>>, Throwable> onError) {
		super();
		this.collection = collection;
		this.ordered = ordered;
		this.size = size;
		this.onError = onError;
		
		count = 0;
		requestsQueue = new LinkedList<>();
	}
	
	@Override
	public void write(WriteModel<Document> request) {
		requestsQueue.offer(request);
		if (++count==size)
			flush();
	}
	
	@Override
	public void flush() {
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
		count = 0;
	}
}
