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

import static io.actor4j.database.mongo.MongoOperations.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.cache.Cache.Entry;
import javax.cache.integration.CacheLoader;
import javax.cache.integration.CacheLoaderException;
import javax.cache.integration.CacheWriter;
import javax.cache.integration.CacheWriterException;

import org.apache.commons.lang3.ClassUtils;
import org.bson.Document;

import com.fasterxml.jackson.core.type.TypeReference;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.Filters;

import io.actor4j.database.mongo.MongoBufferedBulkWriter;

public class MongoCacheLoaderAndWriter<K, V> implements CacheLoader<K, V>, CacheWriter<K, V> {
	public static final String KEY_NAME = "_id";
	public static final String VALUE_NAME = "value";

	protected final MongoClient mongoClient;
	protected final String databaseName;
	protected final String collectionName;
	protected final Class<V> valueType;
	protected final Function<Document, V> valueReadMapper;
	protected final Function<V, ?> valueWriteMapper;
	protected final TypeReference<V> valueTypeReference;

	protected final boolean bulkOrdered;
	protected final int bulkSize;
	protected final MongoBufferedBulkWriter bulkWriter;

	public MongoCacheLoaderAndWriter(MongoClient mongoClient, String databaseName, String collectionName,
			Class<V> valueType, Function<Document, V> valueReadMapper, Function<V, ?> valueWriteMapper, boolean bulkOrdered,
			int bulkSize) {
		super();
		this.mongoClient = mongoClient;
		this.databaseName = databaseName;
		this.collectionName = collectionName;
		this.valueType = valueType;
		this.valueReadMapper = valueReadMapper;
		this.valueWriteMapper = valueWriteMapper;
		this.valueTypeReference = null;
		this.bulkOrdered = bulkOrdered;
		this.bulkSize = bulkSize;
		if (bulkSize > 0)
			this.bulkWriter = MongoBufferedBulkWriter.create(mongoClient, databaseName, collectionName, bulkOrdered,
					bulkSize);
		else
			this.bulkWriter = null;
	}

	public MongoCacheLoaderAndWriter(MongoClient mongoClient, String databaseName, String collectionName,
			Class<V> valueType, boolean bulkOrdered, int bulkSize) {
		this(mongoClient, databaseName, collectionName, valueType, null, null, bulkOrdered, bulkSize);
	}

	public MongoCacheLoaderAndWriter(MongoClient mongoClient, String databaseName, String collectionName,
			TypeReference<V> valueTypeReference, boolean bulkOrdered, int bulkSize) {
		super();
		this.mongoClient = mongoClient;
		this.databaseName = databaseName;
		this.collectionName = collectionName;
		this.valueType = null;
		this.valueReadMapper = null;
		this.valueWriteMapper = null;
		this.valueTypeReference = valueTypeReference;
		this.bulkOrdered = bulkOrdered;
		this.bulkSize = bulkSize;
		if (bulkSize > 0)
			this.bulkWriter = MongoBufferedBulkWriter.create(mongoClient, databaseName, collectionName, bulkOrdered,
					bulkSize);
		else
			this.bulkWriter = null;
	}

	public MongoCacheLoaderAndWriter(MongoClient mongoClient, String databaseName, String collectionName,
			Class<V> valueType, int bulkSize) {
		this(mongoClient, databaseName, collectionName, valueType, false, bulkSize);
	}

	public MongoCacheLoaderAndWriter(MongoClient mongoClient, String databaseName, String collectionName,
			Class<V> valueType) {
		this(mongoClient, databaseName, collectionName, valueType, false, -1);
	}

	public MongoCacheLoaderAndWriter(MongoClient mongoClient, String databaseName, String collectionName,
			TypeReference<V> valueTypeReference, int bulkSize) {
		this(mongoClient, databaseName, collectionName, valueTypeReference, false, bulkSize);
	}

	public MongoCacheLoaderAndWriter(MongoClient mongoClient, String databaseName, String collectionName,
			TypeReference<V> valueTypeReference) {
		this(mongoClient, databaseName, collectionName, valueTypeReference, false, -1);
	}

	@SuppressWarnings("unchecked")
	@Override
	public V load(K key) throws CacheLoaderException {
		V result = null;

		Document document = findOne(Filters.eq(KEY_NAME, key), mongoClient, databaseName, collectionName);
		if (document != null && document.containsKey(VALUE_NAME)) {
			Object obj = document.get(VALUE_NAME);
			if (obj instanceof Document d) {
				if (valueType != null)
					result = valueReadMapper!=null ? valueReadMapper.apply(d) : convertToValue(d, valueType);
				else
					result = convertToValue(d, valueTypeReference);
			} else
				result = (V) obj;
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<K, V> loadAll(Iterable<? extends K> keys) throws CacheLoaderException {
		Map<K, V> result = new HashMap<>();

		List<Document> documents = findAll(Filters.in(KEY_NAME, keys), null, null, mongoClient, databaseName, collectionName);
		documents.stream().forEach((document) -> {
			K key = null;
			V value = null;

			if (document.containsKey(KEY_NAME))
				key = (K) document.get(KEY_NAME);
			if (document.containsKey(VALUE_NAME)) {
				Object obj = document.get(VALUE_NAME);
				if (obj instanceof Document d) {
					if (valueType != null)
						value = valueReadMapper!=null ? valueReadMapper.apply(d) : convertToValue(d, valueType);
					else
						value = convertToValue(d, valueTypeReference);
				} else
					value = (V) obj;
			}

			if (key != null && value != null)
				result.put(key, value);
		});

		return result;
	}

	@Override
	public void write(Entry<? extends K, ? extends V> entry) throws CacheWriterException {
		Document document = new Document().append(KEY_NAME, entry.getKey());
		if (ClassUtils.isPrimitiveOrWrapper(entry.getValue().getClass()) || entry.getValue() instanceof String)
			document.append(VALUE_NAME, valueWriteMapper!=null ? valueWriteMapper.apply(entry.getValue()) : entry.getValue());
		else
			document.append(VALUE_NAME, valueWriteMapper!=null ? valueWriteMapper.apply(entry.getValue()) : convertToDocument(entry.getValue()));

		insertOne(document, mongoClient, databaseName, collectionName, bulkWriter);
	}

	@Override
	public void writeAll(Collection<Entry<? extends K, ? extends V>> entries) throws CacheWriterException {
		entries.stream().forEach((entry) -> write(entry));
	}

	@Override
	public void delete(Object key) throws CacheWriterException {
		deleteOne(Filters.eq(KEY_NAME, key), mongoClient, databaseName, collectionName, bulkWriter);
	}

	@Override
	public void deleteAll(Collection<?> keys) throws CacheWriterException {
		keys.stream().forEach((key) -> delete(key));
	}

	public void flush() {
		if (bulkWriter != null)
			bulkWriter.flush();
	}
}
