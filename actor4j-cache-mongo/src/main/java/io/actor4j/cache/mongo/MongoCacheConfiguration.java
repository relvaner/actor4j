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
package io.actor4j.cache.mongo;

import java.util.function.Function;

import javax.cache.configuration.MutableConfiguration;

import org.bson.Document;

import com.fasterxml.jackson.core.type.TypeReference;
import com.mongodb.client.MongoClient;

public class MongoCacheConfiguration<K, V> extends MutableConfiguration<K, V> {
	private static final long serialVersionUID = 6602264788991737589L;

	protected final MongoCacheLoaderAndWriter<K, V> cacheLoaderAndWriter;

	public void initialize() {
		setReadThrough(true);
		setWriteThrough(true);
		setCacheLoaderFactory(() -> cacheLoaderAndWriter);
		setCacheWriterFactory(() -> cacheLoaderAndWriter);
	}

	public MongoCacheConfiguration(MongoClient mongoClient, String databaseName, String collectionName,
			Class<V> valueType, Function<Document, V> valueReadMapper, Function<V, ?> valueWriteMapper,
			boolean bulkOrdered, int bulkSize, MongoCacheConfiguration<K, V> configuration) {
		super(configuration);

		cacheLoaderAndWriter = new MongoCacheLoaderAndWriter<>(mongoClient, databaseName, collectionName, valueType,
				valueReadMapper, valueWriteMapper, bulkOrdered, bulkSize);
		initialize();
	}

	public MongoCacheConfiguration(MongoClient mongoClient, String databaseName, String collectionName,
			Class<V> valueType, Function<Document, V> valueReadMapper, Function<V, ?> valueWriteMapper,
			MongoCacheConfiguration<K, V> configuration) {
		this(mongoClient, databaseName, collectionName, valueType, valueReadMapper, valueWriteMapper, false, -1,
				configuration);
	}

	public MongoCacheConfiguration(MongoClient mongoClient, String databaseName, String collectionName,
			Class<V> valueType, MongoCacheConfiguration<K, V> configuration) {
		this(mongoClient, databaseName, collectionName, valueType, null, null, false, -1, configuration);
	}

	public MongoCacheConfiguration(MongoClient mongoClient, String databaseName, String collectionName,
			Class<V> valueType, Function<Document, V> valueReadMapper, Function<V, ?> valueWriteMapper,
			boolean bulkOrdered, int bulkSize) {
		super();

		cacheLoaderAndWriter = new MongoCacheLoaderAndWriter<>(mongoClient, databaseName, collectionName, valueType,
				valueReadMapper, valueWriteMapper, bulkOrdered, bulkSize);
		initialize();
	}

	public MongoCacheConfiguration(MongoClient mongoClient, String databaseName, String collectionName,
			Class<V> valueType, Function<Document, V> valueReadMapper, Function<V, ?> valueWriteMapper) {
		this(mongoClient, databaseName, collectionName, valueType, valueReadMapper, valueWriteMapper, false, -1);
	}

	public MongoCacheConfiguration(MongoClient mongoClient, String databaseName, String collectionName,
			Class<V> valueType) {
		this(mongoClient, databaseName, collectionName, valueType, null, null, false, -1);
	}

	// ---

	public MongoCacheConfiguration(MongoClient mongoClient, String databaseName, String collectionName,
			TypeReference<V> valueTypeReference, boolean bulkOrdered, int bulkSize,
			MongoCacheConfiguration<K, V> configuration) {
		super(configuration);

		cacheLoaderAndWriter = new MongoCacheLoaderAndWriter<>(mongoClient, databaseName, collectionName,
				valueTypeReference, bulkOrdered, bulkSize);
		initialize();
	}

	public MongoCacheConfiguration(MongoClient mongoClient, String databaseName, String collectionName,
			TypeReference<V> valueTypeReference, MongoCacheConfiguration<K, V> configuration) {
		this(mongoClient, databaseName, collectionName, valueTypeReference, false, -1, configuration);
	}

	public MongoCacheConfiguration(MongoClient mongoClient, String databaseName, String collectionName,
			TypeReference<V> valueTypeReference, boolean bulkOrdered, int bulkSize) {
		super();

		cacheLoaderAndWriter = new MongoCacheLoaderAndWriter<>(mongoClient, databaseName, collectionName,
				valueTypeReference, bulkOrdered, bulkSize);
		initialize();
	}

	public MongoCacheConfiguration(MongoClient mongoClient, String databaseName, String collectionName,
			TypeReference<V> valueTypeReference) {
		this(mongoClient, databaseName, collectionName, valueTypeReference, false, -1);
	}

	// ---

	public MongoCacheLoaderAndWriter<K, V> getCacheLoaderAndWriter() {
		return cacheLoaderAndWriter;
	}
}
