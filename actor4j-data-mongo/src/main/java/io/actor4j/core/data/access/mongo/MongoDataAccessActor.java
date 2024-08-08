/*
 * Copyright (c) 2015-2024, David A. Bauer. All rights reserved.
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

import com.mongodb.client.MongoClient;

import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.data.access.DataAccessActor;

public class MongoDataAccessActor<K, V> extends DataAccessActor<K, V> {
	protected MongoDataAccessActorImpl<K, V> impl;
	
	public MongoDataAccessActor(String name, MongoClient client, String databaseName, 
			boolean bulkWrite, boolean bulkOrdered, int bulkSize, Class<V> valueType, int maxFailures, long resetTimeout) {
		super(name, true); // @Stateful
		
		impl = new MongoDataAccessActorImpl<K, V>(this, client, databaseName, bulkWrite, bulkOrdered, bulkSize, valueType, maxFailures, resetTimeout);
	}
	
	public MongoDataAccessActor(MongoClient client, String databaseName, 
			boolean bulkWrite, boolean bulkOrdered, int bulkSize, Class<V> valueType, int maxFailures, long resetTimeout) {
		this(null, client, databaseName, bulkWrite, bulkOrdered, bulkSize, valueType, maxFailures, resetTimeout);
	}
	
	public MongoDataAccessActor(String name, MongoClient client, String databaseName, 
			boolean bulkWrite, boolean bulkOrdered, int bulkSize, Class<V> valueType) {
		this(name, client, databaseName, bulkWrite, bulkOrdered, bulkSize, valueType, DEFAULT_MAX_FAILURES, DEFAULT_RESET_TIMEOUT);
	}
	
	public MongoDataAccessActor(MongoClient client, String databaseName, 
			boolean bulkWrite, boolean bulkOrdered, int bulkSize, Class<V> valueType) {
		this(null, client, databaseName, bulkWrite, bulkOrdered, bulkSize, valueType, DEFAULT_MAX_FAILURES, DEFAULT_RESET_TIMEOUT);
	}
	
	public MongoDataAccessActor(String name, MongoClient client, String databaseName, Class<V> valueType, int maxFailures, long resetTimeout) {
		this(name, client, databaseName, false, true, 0, valueType, maxFailures, resetTimeout);
	}
	
	public MongoDataAccessActor(String name, MongoClient client, String databaseName, Class<V> valueType) {
		this(name, client, databaseName, false, true, 0, valueType, DEFAULT_MAX_FAILURES, DEFAULT_RESET_TIMEOUT);
	}
	
	public MongoDataAccessActor(MongoClient client, String databaseName, Class<V> valueType, int maxFailures, long resetTimeout) {
		this(null, client, databaseName, valueType, maxFailures, resetTimeout);
	}
	
	public MongoDataAccessActor(MongoClient client, String databaseName, Class<V> valueType) {
		this(null, client, databaseName, valueType, DEFAULT_MAX_FAILURES, DEFAULT_RESET_TIMEOUT);
	}
	
	@Override
	public void receive(ActorMessage<?> message) {
		impl.receive(message);
	}
}
