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

import static io.actor4j.database.mongo.MongoOperations.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.cache.Cache.Entry;
import javax.cache.integration.CacheLoader;
import javax.cache.integration.CacheLoaderException;
import javax.cache.integration.CacheWriter;
import javax.cache.integration.CacheWriterException;

import org.apache.commons.lang3.ClassUtils;
import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.WriteModel;

import io.actor4j.core.utils.GenericType;
import io.actor4j.core.utils.Pair;
import io.actor4j.database.mongo.ConcurrentMongoBufferedBulkWriter;
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
	protected final GenericType<V> valueTypeReference;

	protected final boolean bulkOrdered;
	protected final int bulkSize;
	protected final MongoBufferedBulkWriter bulkWriter;

	public MongoCacheLoaderAndWriter(MongoClient mongoClient, String databaseName, String collectionName,
			Class<V> valueType, Function<Document, V> valueReadMapper, Function<V, ?> valueWriteMapper, boolean bulkOrdered,
			int bulkSize, Consumer<List<Pair<UUID, WriteModel<Document>>>> onBulkWriterSuccess, BiConsumer<List<Pair<UUID, WriteModel<Document>>>, Throwable> onBulkWriterError) {
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
			this.bulkWriter = ConcurrentMongoBufferedBulkWriter.create(mongoClient, databaseName, collectionName, bulkOrdered,
					bulkSize, onBulkWriterSuccess, onBulkWriterError);
		else
			this.bulkWriter = null;
	}

	public MongoCacheLoaderAndWriter(MongoClient mongoClient, String databaseName, String collectionName,
			GenericType<V> valueTypeReference, boolean bulkOrdered, int bulkSize, 
			Consumer<List<Pair<UUID, WriteModel<Document>>>> onBulkWriterSuccess, BiConsumer<List<Pair<UUID, WriteModel<Document>>>, Throwable> onBulkWriterError) {
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
			this.bulkWriter = ConcurrentMongoBufferedBulkWriter.create(mongoClient, databaseName, collectionName, bulkOrdered,
					bulkSize, onBulkWriterSuccess, onBulkWriterError);
		else
			this.bulkWriter = null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public V load(K key) throws CacheLoaderException {
		V result = null;

		try {
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
		} catch(Exception e) {
			throw new CacheLoaderException("database error", e);
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<K, V> loadAll(Iterable<? extends K> keys) throws CacheLoaderException {
		Map<K, V> result = new HashMap<>();

		try {
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
		}
		catch(Exception e) {
			throw new CacheLoaderException("database error", e);
		}

		return result;
	}

	@Override
	public void write(Entry<? extends K, ? extends V> entry) throws CacheWriterException {
		try {
			Document document = new Document().append(KEY_NAME, entry.getKey());
			if (ClassUtils.isPrimitiveOrWrapper(entry.getValue().getClass()) || entry.getValue() instanceof String)
				document.append(VALUE_NAME, valueWriteMapper!=null ? valueWriteMapper.apply(entry.getValue()) : entry.getValue());
			else
				document.append(VALUE_NAME, valueWriteMapper!=null ? valueWriteMapper.apply(entry.getValue()) : convertToDocument(entry.getValue()));
	
			insertOne(document, UUID.randomUUID(), mongoClient, databaseName, collectionName, bulkWriter);
		}
		catch(Exception e) {
			throw new CacheWriterException("database error", e);
		}
	}

	@Override
	public void writeAll(Collection<Entry<? extends K, ? extends V>> entries) throws CacheWriterException {
		entries.stream().forEach((entry) -> write(entry));
	}

	@Override
	public void delete(Object key) throws CacheWriterException {
		try {
			deleteOne(Filters.eq(KEY_NAME, key), UUID.randomUUID(), mongoClient, databaseName, collectionName, bulkWriter);
		}
		catch(Exception e) {
			throw new CacheWriterException("database error", e);
		}
	}

	@Override
	public void deleteAll(Collection<?> keys) throws CacheWriterException {
		keys.stream().forEach((key) -> delete(key));
	}

	public void flush() throws CacheWriterException {
		if (bulkWriter != null)
			bulkWriter.flush();
		else
			throw new CacheWriterException("bulkWriter is null");
	}
}
