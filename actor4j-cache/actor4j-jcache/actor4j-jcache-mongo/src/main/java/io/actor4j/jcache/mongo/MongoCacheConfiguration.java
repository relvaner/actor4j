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
package io.actor4j.jcache.mongo;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.cache.configuration.MutableConfiguration;

import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.model.WriteModel;

import io.actor4j.core.utils.GenericType;
import io.actor4j.core.utils.Pair;
import io.actor4j.jcache.mongo.runtime.AsyncMongoCacheLoaderAndWriter;

public class MongoCacheConfiguration<K, V> extends MutableConfiguration<K, V> {
	private static final long serialVersionUID = 6602264788991737589L;

	protected MongoClient mongoClient;
	protected String databaseName;
	protected String collectionName;
	protected Class<V> valueType;
	protected Function<Document, V> valueReadMapper;
	protected Function<V, ?> valueWriteMapper;
	protected GenericType<V> valueTypeReference;

	protected boolean bulkOrdered;
	protected int bulkSize = -1;
	protected Consumer<List<Pair<UUID, WriteModel<Document>>>> onBulkWriterSuccess;
	protected BiConsumer<List<Pair<UUID, WriteModel<Document>>>, Throwable> onBulkWriterError;
	
	protected AsyncMongoCacheLoaderAndWriter<K, V> cacheLoaderAndWriter;
	
	protected BiConsumer<Pair<K, V>, Throwable> asyncLoadHandler;
	protected BiConsumer<Map<K,V>, Throwable> asyncLoadAllHandler;

	public MongoClient getMongoClient() {
		return mongoClient;
	}

	public MongoCacheConfiguration<K, V> setMongoClient(MongoClient mongoClient) {
		this.mongoClient = mongoClient;
		return this;
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public MongoCacheConfiguration<K, V> setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
		return this;
	}

	public String getCollectionName() {
		return collectionName;
	}

	public MongoCacheConfiguration<K, V> setCollectionName(String collectionName) {
		this.collectionName = collectionName;
		return this;
	}

	public Class<V> getValueType() {
		return valueType;
	}

	public MongoCacheConfiguration<K, V> setValueType(Class<V> valueType) {
		this.valueType = valueType;
		return this;
	}

	public Function<Document, V> getValueReadMapper() {
		return valueReadMapper;
	}

	public MongoCacheConfiguration<K, V> setValueReadMapper(Function<Document, V> valueReadMapper) {
		this.valueReadMapper = valueReadMapper;
		return this;
	}

	public Function<V, ?> getValueWriteMapper() {
		return valueWriteMapper;
	}

	public MongoCacheConfiguration<K, V> setValueWriteMapper(Function<V, ?> valueWriteMapper) {
		this.valueWriteMapper = valueWriteMapper;
		return this;
	}

	public GenericType<V> getValueTypeReference() {
		return valueTypeReference;
	}

	public MongoCacheConfiguration<K, V> setValueTypeReference(GenericType<V> valueTypeReference) {
		this.valueTypeReference = valueTypeReference;
		return this;
	}

	public boolean isBulkOrdered() {
		return bulkOrdered;
	}

	public MongoCacheConfiguration<K, V> setBulkOrdered(boolean bulkOrdered) {
		this.bulkOrdered = bulkOrdered;
		return this;
	}

	public int getBulkSize() {
		return bulkSize;
	}

	public MongoCacheConfiguration<K, V> setBulkSize(int bulkSize) {
		this.bulkSize = bulkSize;
		return this;
	}
	
	public Consumer<List<Pair<UUID, WriteModel<Document>>>> getOnBulkWriterSuccess() {
		return onBulkWriterSuccess;
	}

	public void setOnBulkWriterSuccess(Consumer<List<Pair<UUID, WriteModel<Document>>>> onBulkWriterSuccess) {
		this.onBulkWriterSuccess = onBulkWriterSuccess;
	}
	
	public BiConsumer<List<Pair<UUID, WriteModel<Document>>>, Throwable> getOnBulkWriterError() {
		return onBulkWriterError;
	}

	public void setOnBulkWriterError(BiConsumer<List<Pair<UUID, WriteModel<Document>>>, Throwable> onBulkWriterError) {
		this.onBulkWriterError = onBulkWriterError;
	}

	public AsyncMongoCacheLoaderAndWriter<K, V> getCacheLoaderAndWriter() {
		return cacheLoaderAndWriter;
	}
	
	// ---

	public BiConsumer<Pair<K, V>, Throwable> getAsyncLoadHandler() {
		return asyncLoadHandler;
	}

	public void setAsyncLoadHandler(BiConsumer<Pair<K, V>, Throwable> asyncLoadHandler) {
		this.asyncLoadHandler = asyncLoadHandler;
	}

	public BiConsumer<Map<K, V>, Throwable> getAsyncLoadAllHandler() {
		return asyncLoadAllHandler;
	}

	public void setAsyncLoadAllHandler(BiConsumer<Map<K, V>, Throwable> asyncLoadAllHandler) {
		this.asyncLoadAllHandler = asyncLoadAllHandler;
	}
	
	// ---

	public MongoCacheConfiguration<K, V> build() {
		if (valueType!=null)
			cacheLoaderAndWriter = new AsyncMongoCacheLoaderAndWriter<K, V>(mongoClient, databaseName, collectionName, 
				valueType, valueReadMapper, valueWriteMapper, bulkOrdered, bulkSize, onBulkWriterSuccess, onBulkWriterError);
		else if (valueTypeReference!=null)
			cacheLoaderAndWriter = new AsyncMongoCacheLoaderAndWriter<K, V>(mongoClient, databaseName, collectionName, 
				valueTypeReference, bulkOrdered, bulkSize, onBulkWriterSuccess, onBulkWriterError);
		
		cacheLoaderAndWriter.setAsyncLoadHandler(asyncLoadHandler);
		cacheLoaderAndWriter.setAsyncLoadAllHandler(asyncLoadAllHandler);
		
		setReadThrough(true);
		setWriteThrough(true);
		setCacheLoaderFactory(() -> cacheLoaderAndWriter);
		setCacheWriterFactory(() -> cacheLoaderAndWriter);
			
		return this;
	}
}
