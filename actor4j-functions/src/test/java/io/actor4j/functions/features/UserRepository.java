/*
 * Copyright (c) 2015-2026, David A. Bauer. All rights reserved.
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
package io.actor4j.functions.features;

import java.util.HashMap;
import java.util.Map;

record User(Integer id, String name, String email) {
}

public class UserRepository {
	protected final Map<Integer, User> storage = new HashMap<>();

	public UserRepository() {
		storage.put(1, new User(1, "John Doe", "john.doe@example.com"));
		storage.put(2, new User(2, "Jane Doe", "jane.doe@example.com"));
	}

	public User findById(Integer id) {
		return storage.get(id);
	}

	public User save(Integer id, String name, String email) {
		User user = new User(id, name, email);
		storage.put(id, user);
		return user;
	}

	public static String schema() {
		return """
			type User {
				id: Int!
				name: String!
				email: String
			}
			input CreateUserInput {
				id: Int!
				name: String!
				email: String!
			}
			type Query {
				getUser(id: Int!): User
			}
			type Mutation {
				createUser(user: CreateUserInput!): User
			}
		""";
	}
}