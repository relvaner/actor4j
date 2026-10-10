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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import graphql.GraphQL;
import graphql.execution.preparsed.PreparsedDocumentEntry;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.Cache;
import io.actor4j.core.utils.CacheAsMap;
import io.actor4j.core.utils.CacheLRU;
import io.actor4j.core.utils.Reply;
import io.actor4j.data.publish.subscribe.Publish;
import io.actor4j.data.publish.subscribe.Subscribe;
import io.actor4j.functions.pods.FunctionPod;
import io.actor4j.functions.pods.FunctionPodContext;
import io.actor4j.functions.pods.PodFunction;
import io.actor4j.functions.query.AsyncQueryRequest;
import io.actor4j.functions.query.AsyncQueryRequestHandler;
import io.actor4j.functions.state.StateStore;
import io.actor4j.functions.streams.AsyncStreams;
import io.actor4j.functions.streams.AsyncStreamsHandler;

import static io.actor4j.core.logging.ActorLogger.*;

public class ExampleFunctionPod extends FunctionPod {
	protected static final ObjectMapper objectMapper = new ObjectMapper();
	
	protected final UserRepository repository;
	
	protected final Cache<String, PreparsedDocumentEntry> preparsedDocumentEntryCache;
	protected GraphQL graphQL;

	public ExampleFunctionPod() {
		super();
		
		repository = new UserRepository();
		preparsedDocumentEntryCache = new CacheLRU<>(1_000);
	}

	@Override
	public String domain() {
		return "ExampleFunctionPod";
	}
	
	@Override
	public StateStore<?, ?> createStateStore(FunctionPodContext contextFunctions) {
		return new StateStore<String, Integer>(new CacheAsMap<>());
	}
	
	@Override
	public AsyncStreams createStreams(FunctionPodContext contextFunctions) {
		return new AsyncStreams(new AsyncStreamsHandler() {
			@Override
			public void publish(String topic, Object value) {
				contextFunctions.host().sendViaAlias(
					ActorMessage.create(
						new Publish<Object>(topic, value), 0, 
						contextFunctions.host().self(), null), 
					"broker");
			}

			@Override
			public void subscribe(String topic) {
				contextFunctions.host().sendViaAlias(
					ActorMessage.create(new Subscribe(topic), 0, contextFunctions.host().self(), null), "broker");
			}

			@Override
			public void unsubscribe(String topic) {
			}
		});
	}
	
	public AsyncQueryRequest createQueryRequest(FunctionPodContext contextFunctions) {
		TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(UserRepository.schema());
		RuntimeWiring.Builder wiringBuilder = RuntimeWiring.newRuntimeWiring();
		
		UserGraphQLHandler handler = new UserGraphQLHandler(repository);
        handler.configure(wiringBuilder);
		
        GraphQLSchema graphQLSchema = new SchemaGenerator().makeExecutableSchema(typeRegistry, wiringBuilder.build());
        graphQL = GraphQL.newGraphQL(graphQLSchema)
        		.preparsedDocumentProvider((executionInput, parseAndValidate) -> {
        			String query = executionInput.getQuery();
        			PreparsedDocumentEntry entry = preparsedDocumentEntryCache.get(query);
        			if (entry==null) {
        				entry = parseAndValidate.apply(executionInput);
        				preparsedDocumentEntryCache.put(query, entry);
        			}
        				
        			return CompletableFuture.completedFuture(entry);
        		})
        		.build();

		return new AsyncQueryRequest(new AsyncQueryRequestHandler() {
			@Override
			public Object execute(Object value) {
				return handler.execute(value, graphQL);
			}});
	}
	
	public void preStart(FunctionPodContext contextFunction) {
		contextFunction.stateStore().put("result", 41);
		contextFunction.streams().subscribe("MyTopic_in");
	}
	
	public static Object gql(String operationType, String operationName, Object variables, Object selection, FunctionPodContext ctx) {
		Map<String, Object> map = Map.of("operationType", operationType, "operationName", operationName, 
			"variables", objectMapper.convertValue(variables, new TypeReference<Map<String, Object>>() {}), 
			"selection", selection);
		return ctx.queryRequest().execute(map);
	}
	
	public static Object gql(String operationType, String operationName, Object variables, FunctionPodContext ctx) {
		Map<String, Object> map = Map.of("operationType", operationType, "operationName", operationName, 
			"variables", objectMapper.convertValue(variables, new TypeReference<Map<String, Object>>() {}));
		
		return ctx.queryRequest().execute(map);
	}

	@Override
	public PodFunction createPodFunction(FunctionPodContext ctx) {
		return new PodFunction(ctx) {
			@Override
			public Reply handle(ActorMessage<?> message) {
				StateStore<String, Integer> stateStore = ctx.stateStore();
				
				logger().log(INFO, "welcome");
				logger().log(INFO, message.value().toString());
				
				logger().log(INFO, ctx.stateStore().get("result").toString());
				stateStore.put("result", stateStore.get("result")+1);
				ctx.streams().publish("MyTopic_out", stateStore.get("result")+1);
				
				final int id = stateStore.get("result");
				User user = new User(id, "Alice Smith", "alice@example.com");
				gql("mutation", "createUser", Map.of("user", user), ctx);
				
				final UUID requestId = (UUID)gql("query", "getUser", Map.of("id", id), ctx);
				logger().log(INFO, requestId.toString());
				
				return Reply.none();
			}
		};
	}
}
