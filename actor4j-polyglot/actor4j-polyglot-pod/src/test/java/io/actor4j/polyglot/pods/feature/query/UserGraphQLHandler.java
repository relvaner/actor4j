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
package io.actor4j.polyglot.pods.feature.query;

import graphql.schema.idl.RuntimeWiring.Builder;
import io.actor4j.query.graphql.DefaultGraphQLHandler;

import java.util.Map;
import java.util.UUID;

public class UserGraphQLHandler extends DefaultGraphQLHandler {
	private final UserRepository repository;

	public UserGraphQLHandler(UserRepository repository) {
		this.repository = repository;
	}

	@Override
	public void configure(Builder builder) {
		builder.type("Query", typeWiring -> typeWiring.dataFetcher("getUser", env -> {
			Integer id = env.getArgument("id");
			return repository.findById(id);
		}));

		builder.type("Mutation", typeWiring -> typeWiring.dataFetcher("createUser", env -> {
			Map<String, Object> user = env.getArgument("user");
			Integer id = (Integer) user.get("id");
			String name = (String) user.get("name");
			String email = (String) user.get("email");
			return repository.save(id, name, email);
		}));
	}

	@Override
	public void handleAsyncResponse(UUID requestId, Map<String, Object> responseMap) {
		System.out.println(responseMap);
	}
}
