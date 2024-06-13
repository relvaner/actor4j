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
package io.actor4j.jcache.mongo.runtime;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.bson.Document;

import com.mongodb.client.MongoClient;

import io.actor4j.core.utils.GenericType;
import io.actor4j.jcache.AsyncCacheLoader;
import io.actor4j.jcache.mongo.MongoCacheLoaderAndWriter;

public class AsyncMongoCacheLoaderAndWriter<K, V> extends MongoCacheLoaderAndWriter<K, V> implements AsyncCacheLoader<K, V> {
	protected BiConsumer<K, V> asyncLoadHandler;
	protected Consumer<Map<K,V>> asyncLoadAllHandler;

	public AsyncMongoCacheLoaderAndWriter(MongoClient mongoClient, String databaseName, String collectionName,
			Class<V> valueType, Function<Document, V> valueReadMapper, Function<V, ?> valueWriteMapper,
			boolean bulkOrdered, int bulkSize) {
		super(mongoClient, databaseName, collectionName, valueType, valueReadMapper, valueWriteMapper, bulkOrdered, bulkSize);
	}

	public AsyncMongoCacheLoaderAndWriter(MongoClient mongoClient, String databaseName, String collectionName,
			GenericType<V> valueTypeReference, boolean bulkOrdered, int bulkSize) {
		super(mongoClient, databaseName, collectionName, valueTypeReference, bulkOrdered, bulkSize);
	}

	@Override
	public CompletableFuture<V> asyncLoad(K key, Executor executor) {
		CompletableFuture<V> result = new CompletableFuture<>();
		executor.execute(() -> {
			V value = load(key);
			if (asyncLoadHandler!=null)
				asyncLoadHandler.accept(key, value);
			result.complete(value);
		});
		
		return result;
	}

	@Override
	public CompletableFuture<Map<K,V>> asyncLoadAll(Iterable<? extends K> keys, Executor executor) {
		CompletableFuture<Map<K,V>> result = new CompletableFuture<>();
		executor.execute(() -> {
			Map<K,V> map = loadAll(keys);
			if (asyncLoadAllHandler!=null)
				asyncLoadAllHandler.accept(map);
			result.complete(map);
		});
		
		return result;
	}

	public void setAsyncLoadHandler(BiConsumer<K, V> asyncLoadHandler) {
		this.asyncLoadHandler = asyncLoadHandler;
	}

	public void setAsyncLoadAllHandler(Consumer<Map<K, V>> asyncLoadAllHandler) {
		this.asyncLoadAllHandler = asyncLoadAllHandler;
	}
}
