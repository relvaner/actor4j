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
import java.util.UUID;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;

public abstract class DefaultGraphQLHandler implements GraphQLHandler {
	@SuppressWarnings("unchecked")
	@Override
	public UUID  execute(Object value, GraphQL graphQL) {
		String document = null;
		Map<String, Object> rawVariables = Map.of();
		String operationType = null;
		String operationName = null;
		String selection = null;

		if (value instanceof String str)
			document = str;
		else if (value instanceof Map<?, ?> map) {
			if (map.get("query") instanceof String q)
				document = q;
			else if (map.get("mutation") instanceof String m)
				document = m;
			else if (map.get("subscription") instanceof String sub)
				document = sub;

			if (map.get("variables") instanceof Map<?, ?> vars)
				rawVariables = (Map<String, Object>) vars;

			if (map.get("operationType") instanceof String t)
				operationType = t;

			if (map.get("operationName") instanceof String op)
				operationName = op;

			if (map.get("selection") instanceof String sel)
				selection = sel;
		}

		if (document == null && operationType != null && operationName != null)
			document = GraphQLOperationGenerator.build(graphQL.getGraphQLSchema(), operationType, operationName, selection);

		if (document == null)
			throw new IllegalArgumentException("Invalid payload: Missing 'query', 'mutation', 'subscription' or known 'operationType'/'operationName'.");

		ExecutionInput.Builder executionInputBuilder = ExecutionInput
			.newExecutionInput()
			.query(document)
			.variables(rawVariables);

		if (operationName != null)
			executionInputBuilder.operationName(operationName);

		final UUID requestId = UUID.randomUUID();
		graphQL.executeAsync(executionInputBuilder.build()).thenAccept(result -> {
			if (!result.getErrors().isEmpty())
				System.err.println("GraphQL Errors: " + result.getErrors());

			if (result.getData() instanceof Publisher<?> publisher)
				subscribe(requestId, (Publisher<ExecutionResult>) publisher);
			else
				handleAsyncResponse(requestId, result.toSpecification());
		});
		
		return requestId;
	}

	protected void subscribe(UUID requestId, Publisher<ExecutionResult> publisher) {
		publisher.subscribe(new Subscriber<ExecutionResult>() {
			@Override
			public void onSubscribe(Subscription subscription) {
				subscription.request(Long.MAX_VALUE);
			}

			@Override
			public void onNext(ExecutionResult event) {
				handleAsyncSubscription(requestId, event.toSpecification());
			}

			@Override
			public void onError(Throwable throwable) {
				System.err.println("GraphQL Subscription Error: " + throwable);
			}

			@Override
			public void onComplete() {
			}
		});
	}

	public void handleAsyncResponse(UUID requestId, Map<String, Object> responseMap) {
		// empty
	}
	
	public void handleAsyncSubscription(UUID requestId, Map<String, Object> responseMap) {
		// empty
	}
}