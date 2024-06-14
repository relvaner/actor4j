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
package io.actor4j.database.mongo;

import java.util.List;
import java.util.function.BiConsumer;

import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.WriteModel;

public interface MongoBufferedBulkWriter {
	public void write(WriteModel<Document> request);
	public void flush();
	
	public static MongoBufferedBulkWriter create(MongoClient client, String databaseName, String collectionName, boolean ordered, int size, 
			BiConsumer<List<WriteModel<Document>>, Throwable> onError) {
		MongoBufferedBulkWriter result = null;

		try {
			MongoCollection<Document> collection = client.getDatabase(databaseName).getCollection(collectionName);
			result =  new MongoBufferedBulkWriterImpl(collection, ordered, size, onError);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
}
