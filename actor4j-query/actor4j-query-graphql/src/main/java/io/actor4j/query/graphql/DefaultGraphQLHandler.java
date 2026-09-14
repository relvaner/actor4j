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
package io.actor4j.query.graphql;

import java.util.Map;

import graphql.ExecutionInput;
import graphql.GraphQL;

public abstract class DefaultGraphQLHandler implements GraphQLHandler {
	protected final GraphQL graphQL;

	public DefaultGraphQLHandler(GraphQL graphQL) {
		this.graphQL = graphQL;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void execute(Object value) {
		String queryOrMutation = null;
		Map<String, Object>  rawVariables = Map.of();
		String operationName = null;

		if (value instanceof String str)
			queryOrMutation = str;
		else if (value instanceof Map<?, ?> map) {
			if (map.get("query") instanceof String q)
				queryOrMutation = q;
			else if (map.get("mutation") instanceof String m)
				queryOrMutation = m;

			if (map.get("variables") instanceof Map<?, ?> vars)
				 rawVariables = (Map<String, Object>) vars;

			if (map.get("operationName") instanceof String op)
				operationName = op;
		}

		if (queryOrMutation != null) {
			ExecutionInput.Builder executionInputBuilder = ExecutionInput
				.newExecutionInput()
				.query(queryOrMutation)
				.variables(rawVariables);

			if (operationName != null)
				executionInputBuilder.operationName(operationName);

			graphQL.executeAsync(executionInputBuilder.build()).thenAccept(result -> {
				Map<String, Object> responseMap = result.toSpecification();
				handleAsyncResponse(responseMap);
			});
		} 
		else
			throw new IllegalArgumentException("Invalid payload: Missing 'query' or 'mutation' string.");
	}
	
	public abstract void handleAsyncResponse(Map<String, Object> responseMap);
}
