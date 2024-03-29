/*
 * Copyright (c) 2015-2018, David A. Bauer. All rights reserved.
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
package io.actor4j.core.persistence.drivers.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import io.actor4j.core.ActorSystem;
import io.actor4j.core.persistence.drivers.PersistenceDriver;
import io.actor4j.core.persistence.drivers.PersistenceImpl;

public class MongoDBPersistenceDriver extends PersistenceDriver {
	protected MongoClient client;
	
	public MongoDBPersistenceDriver(String host, int port, String databaseName) {
		super(host, port, databaseName);
	}
	
	@Override
	public void open() {
		if (client==null)
			client = MongoClients.create("mongodb://"+host+":"+port);
	}

	@Override
	public void close() {
		client.close();
	}

	@Override
	public PersistenceImpl createPersistenceImpl(ActorSystem parent) {
		return new MongoDBPersistenceImpl(parent, this);
	}
}
