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
package io.actor4j.core.mongo;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.DeleteOneModel;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.UpdateOneModel;

public final class MongoOperations {
	public static boolean hasOne(Bson filter, MongoClient client, String databaseName, String collectionName) {
		boolean result = false;
		
		try {
			MongoCollection<Document> collection = client.getDatabase(databaseName).getCollection(collectionName);
			Document document = collection.find(filter).first();
			
			if (document!=null)
				result = true;
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public static void insertOne(Document document, MongoClient client, String databaseName, String collectionName) {
		insertOne(document, client, databaseName, collectionName, null);
	}
	
	public static void replaceOne(Bson filter, Document document, MongoClient client, String databaseName, String collectionName) {
		replaceOne(filter, document, client, databaseName, collectionName, null);
	}
	
	public static void updateOne(Bson filter, Bson update, MongoClient client, String databaseName, String collectionName) {
		updateOne(filter, update, client, databaseName, collectionName, null);
	}
	
	public static void deleteOne(Bson filter, MongoClient client, String databaseName, String collectionName) {
		deleteOne(filter, client, databaseName, collectionName, null);
	}
	
	public static void insertOne(Document document, MongoClient client, String databaseName, String collectionName, MongoBufferedBulkWriter bulkWriter) {
		if (bulkWriter!=null)
			bulkWriter.write(new InsertOneModel<>(document));
		else {
			try {
				MongoCollection<Document> collection = client.getDatabase(databaseName).getCollection(collectionName);
				collection.insertOne(document);
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void replaceOne(Bson filter, Document document, MongoClient client, String databaseName, String collectionName, MongoBufferedBulkWriter bulkWriter) {
		if (bulkWriter!=null)
			bulkWriter.write(new ReplaceOneModel<>(filter, document));
		else {
			try {
				MongoCollection<Document> collection = client.getDatabase(databaseName).getCollection(collectionName);
				collection.replaceOne(filter, document);
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void updateOne(Bson filter, Bson update, MongoClient client, String databaseName, String collectionName, MongoBufferedBulkWriter bulkWriter) {
		if (bulkWriter!=null)
			bulkWriter.write(new UpdateOneModel<>(filter, update));
		else {
			try {
				MongoCollection<Document> collection = client.getDatabase(databaseName).getCollection(collectionName);
				collection.updateOne(filter, update);
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void deleteOne(Bson filter, MongoClient client, String databaseName, String collectionName, MongoBufferedBulkWriter bulkWriter) {
		if (bulkWriter!=null)
			bulkWriter.write(new DeleteOneModel<>(filter));
		else {
			try {
				MongoCollection<Document> collection = client.getDatabase(databaseName).getCollection(collectionName);
				collection.deleteOne(filter);
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static Document findOne(Bson filter, MongoClient client, String databaseName, String collectionName) {
		Document result = null;
		
		try {
			MongoCollection<Document> collection = client.getDatabase(databaseName).getCollection(collectionName);
			result = collection.find(filter).first();
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		return result;
	}
	
	public static List<Document> find(Bson filter, Bson sort, Bson projection, int skip, int limit, MongoClient client, String databaseName, String collectionName) {
		List<Document> result = new LinkedList<>();
		
		try {
			MongoCollection<Document> collection = client.getDatabase(databaseName).getCollection(collectionName);
			
			FindIterable<Document> iterable = null;
			if (filter!=null)
				iterable = collection.find(filter);
			else
				iterable = collection.find();
			
			if (sort!=null)
				iterable = iterable.sort(sort);
			if (projection!=null)
				iterable = iterable.projection(projection);
			if (skip>0)
				iterable = iterable.skip(skip);
			if (limit>0)
				iterable = iterable.limit(limit);
			
			iterable.forEach((Consumer<? super Document>) document -> {result.add(document);});
			//iterable.forEach((Block<Document>) document -> {result.add(document);});
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public static List<Document> findAll(Bson filter, Bson sort, Bson projection, MongoClient client, String databaseName, String collectionName) {
		return find(filter, sort, projection, 0, 0, client, databaseName, collectionName);
	}
	
	public static <V> Document convertToDocument(V value) {
		Document result = null;
		try {
			result = Document.parse(new ObjectMapper().writeValueAsString(value));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public static <V> V convertToValue(String json, Class<V> valueType) {
		V result = null;
		
		if (json!=null)
			try {
				ObjectMapper objectMapper = new ObjectMapper();
				objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
				
				result = objectMapper.readValue(json, valueType);
			} catch (Exception e) {
				e.printStackTrace();
			}
		
		
		return result;
	}
	
	public static <V> V convertToValue(Document document, Class<V> valueType) {
		V result = null;
		
		if (document!=null)
			result = convertToValue(document.toJson(), valueType);

		return result;
	}
	
	public static <V> List<V> convertToValue(List<Document> documents, Class<V> valueType) {
		List<V> result = new LinkedList<>();
		
		for (Document document : documents)
			try {
				ObjectMapper objectMapper = new ObjectMapper();
				objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
				
				result.add(objectMapper.readValue(document.toJson(), valueType));
			} catch (Exception e) {
				e.printStackTrace();
			}
		
		return result;
	}
}
