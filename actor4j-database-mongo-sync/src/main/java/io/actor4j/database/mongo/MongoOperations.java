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
import java.util.UUID;
import java.util.function.Consumer;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.DeleteOneModel;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.UpdateOneModel;

import io.actor4j.core.json.ObjectMapper;
import io.actor4j.core.utils.GenericType;

public final class MongoOperations {
	private static final ObjectMapper objectMapper = ObjectMapper.create();
	
	public static boolean hasOne(Bson filter, MongoClient client, String databaseName, String collectionName) {
		boolean result = false;
		
		MongoCollection<Document> collection = client.getDatabase(databaseName).getCollection(collectionName);
		Document document = collection.find(filter).first();
		
		if (document!=null)
			result = true;
		
		return result;
	}
	
	public static void insertOne(Document document, MongoClient client, String databaseName, String collectionName) {
		insertOne(document, null, client, databaseName, collectionName, null);
	}
	
	public static void replaceOne(Bson filter, Document document, MongoClient client, String databaseName, String collectionName) {
		replaceOne(filter, document, null, client, databaseName, collectionName, null);
	}
	
	public static void updateOne(Bson filter, Bson update, MongoClient client, String databaseName, String collectionName) {
		updateOne(filter, update, null, client, databaseName, collectionName, null);
	}
	
	public static void deleteOne(Bson filter, MongoClient client, String databaseName, String collectionName) {
		deleteOne(filter, null, client, databaseName, collectionName, null);
	}
	
	public static void insertOne(Document document, UUID id, MongoBufferedBulkWriter bulkWriter) {
		insertOne(document, id, null, null, null, bulkWriter);
	}
	
	public static void replaceOne(Bson filter, Document document, UUID id, MongoBufferedBulkWriter bulkWriter) {
		replaceOne(filter, document, id, null, null, null, bulkWriter);
	}
	
	public static void updateOne(Bson filter, Bson update, UUID id, MongoBufferedBulkWriter bulkWriter) {
		updateOne(filter, update, id, null, null, null, bulkWriter);
	}
	
	public static void deleteOne(Bson filter, UUID id, MongoBufferedBulkWriter bulkWriter) {
		deleteOne(filter, id, null, null, null, bulkWriter);
	}
	
	public static void insertOne(Document document, UUID id, MongoClient client, String databaseName, String collectionName, MongoBufferedBulkWriter bulkWriter) {
		if (bulkWriter!=null)
			bulkWriter.write(new InsertOneModel<>(document), id);
		else {
			MongoCollection<Document> collection = client.getDatabase(databaseName).getCollection(collectionName);
			collection.insertOne(document);
		}
	}

	public static void replaceOne(Bson filter, Document document, UUID id, MongoClient client, String databaseName, String collectionName, MongoBufferedBulkWriter bulkWriter) {
		if (bulkWriter!=null)
			bulkWriter.write(new ReplaceOneModel<>(filter, document), id);
		else {
			MongoCollection<Document> collection = client.getDatabase(databaseName).getCollection(collectionName);
			collection.replaceOne(filter, document);
		}
	}
	
	public static void updateOne(Bson filter, Bson update, UUID id, MongoClient client, String databaseName, String collectionName, MongoBufferedBulkWriter bulkWriter) {
		if (bulkWriter!=null)
			bulkWriter.write(new UpdateOneModel<>(filter, update), id);
		else {
			MongoCollection<Document> collection = client.getDatabase(databaseName).getCollection(collectionName);
			collection.updateOne(filter, update);
		}
	}

	public static void deleteOne(Bson filter, UUID id, MongoClient client, String databaseName, String collectionName, MongoBufferedBulkWriter bulkWriter) {
		if (bulkWriter!=null)
			bulkWriter.write(new DeleteOneModel<>(filter), id);
		else {
			MongoCollection<Document> collection = client.getDatabase(databaseName).getCollection(collectionName);
			collection.deleteOne(filter);
		}
	}
	
	public static Document findOne(Bson filter, MongoClient client, String databaseName, String collectionName) {
		Document result = null;
		
		MongoCollection<Document> collection = client.getDatabase(databaseName).getCollection(collectionName);
		result = collection.find(filter).first();

		return result;
	}
	
	public static List<Document> findAll(Bson filter, MongoClient client, String databaseName, String collectionName) {
		List<Document> result = new LinkedList<>();;
		
		MongoCollection<Document> collection = client.getDatabase(databaseName).getCollection(collectionName);
		FindIterable<Document> iterable = collection.find(filter);
		
		iterable.forEach((Consumer<? super Document>) document -> {result.add(document);});

		return result;
	}
	
	public static List<Document> find(Bson filter, Bson sort, Bson projection, int skip, int limit, MongoClient client, String databaseName, String collectionName) {
		List<Document> result = new LinkedList<>();
		
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
		
		return result;
	}
	
	public static List<Document> findAll(Bson filter, Bson sort, Bson projection, MongoClient client, String databaseName, String collectionName) {
		return find(filter, sort, projection, 0, 0, client, databaseName, collectionName);
	}
	
	public static <E> Document convertToDocument(E entity) {
		return Document.parse(objectMapper.mapFrom(entity));
	}
	
	public static <V> V convertToValue(Document document, Class<V> valueType) {
		return convertToEntity(document, valueType);
	}
	
	public static <E> E convertToEntity(Document document, Class<E> entityType) {
		E result = null;
		
		if (document!=null)
			result = objectMapper.mapTo(document.toJson(), entityType);

		return result;
	}
	
	public static <E> List<E> convertToEntities(List<Document> documents, Class<E> entityType) {
		List<E> result = new LinkedList<>();
		
		for (Document document : documents)
			result.add(objectMapper.mapTo(document.toJson(), entityType));
		
		return result;
	}
	
	public static <V> V convertToValue(Document document, GenericType<V> valueTypeRef) {
		return convertToEntity(document, valueTypeRef);
	}
	
	public static <E> E convertToEntity(Document document, GenericType<E> entityTypeRef) {
		E result = null;
		
		if (document!=null)
			result = objectMapper.mapTo(document.toJson(), entityTypeRef);

		return result;
	}
	
	public static <E> List<E> convertToEntities(List<Document> documents, GenericType<E> entityTypeRef) {
		List<E> result = new LinkedList<>();
		
		for (Document document : documents)
			result.add(objectMapper.mapTo(document.toJson(), entityTypeRef));
		
		return result;
	}
}
