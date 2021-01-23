/*
 * Copyright (c) 2015-2017, David A. Bauer. All rights reserved.
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
package io.actor4j.core.data.access.mongo;

import java.util.LinkedList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.WriteModel;

public class MongoBufferedBulkWriterImpl implements MongoBufferedBulkWriter {
	protected MongoCollection<Document> collection;
	
	protected List<WriteModel<Document>> requests;
	protected boolean ordered;
	
	protected int size;
	protected int counter;
	
	public MongoBufferedBulkWriterImpl(MongoCollection<Document> collection, boolean ordered, int size) {
		super();
		this.collection = collection;
		this.ordered = ordered;
		this.size = size;
		
		counter = 0;
		requests = new LinkedList<>();
	}
	
	@Override
	public void write(WriteModel<Document> request) {
		requests.add(request);
		counter++;
		if (counter==size)
			flush();
	}
	
	@Override
	public void flush() {
		if (ordered)
			collection.bulkWrite(requests);
		else
			collection.bulkWrite(requests, new BulkWriteOptions().ordered(false));
		
		requests.clear();
		counter = 0;
	}
}
