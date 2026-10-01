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

import org.graalvm.polyglot.Value;

import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.publish.subscribe.Publish;
import io.actor4j.core.publish.subscribe.Subscribe;
import io.actor4j.core.utils.CacheAsMap;
import io.actor4j.polyglot.api.ValueMapper;
import io.actor4j.polyglot.pods.PolyglotContext;
import io.actor4j.polyglot.pods.PolyglotFunctionPod;
import io.actor4j.polyglot.query.PolyglotQueryRequest;
import io.actor4j.polyglot.query.PolyglotQueryRequestHandler;
import io.actor4j.polyglot.state.PolyglotStateStore;
import io.actor4j.polyglot.streams.PolyglotStreams;
import io.actor4j.polyglot.streams.PolyglotStreamsHandler;

public class ExamplePolyglotFunctionPod_Query_JS extends PolyglotFunctionPod {
	protected final UserRepository repository = new UserRepository();
	protected GraphQL graphQL;

	@Override
	public String domain() {
		return "ExamplePolyglotFunctionPod_Query_JS";
	}

	@Override
	public String languageId() {
		return PolyglotContext.LANGUAGE_ID_JS;
	}
	
	@Override
	public PolyglotStateStore createStateStore() {
		return new PolyglotStateStore(new CacheAsMap<>());
	}
	
	@Override
	public PolyglotStreams createStreams(PolyglotContext contextPolyglot) {
		return new PolyglotStreams(new PolyglotStreamsHandler() {
			@Override
			public void publish(String topic, Value value) {
				Object mappedObject = ValueMapper.convertValue(value);
				
				contextPolyglot.api().host().sendViaAlias(
					ActorMessage.create(
						new Publish<Object>(topic, mappedObject), 0, 
						contextPolyglot.api().host().self(), null), 
					"broker");
			}

			@Override
			public void subscribe(String topic) {
				contextPolyglot.api().host().sendViaAlias(
					ActorMessage.create(new Subscribe(topic), 0, contextPolyglot.api().host().self(), null), "broker");
			}

			@Override
			public void unsubscribe(String topic) {
			}
		});
	}
	
	public PolyglotQueryRequest createQueryRequest() {
		TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(UserRepository.schema());
		RuntimeWiring.Builder wiringBuilder = RuntimeWiring.newRuntimeWiring();
		
		UserGraphQLHandler handler = new UserGraphQLHandler(repository);
        handler.configure(wiringBuilder);
		
        GraphQLSchema graphQLSchema = new SchemaGenerator().makeExecutableSchema(typeRegistry, wiringBuilder.build());
        graphQL = GraphQL.newGraphQL(graphQLSchema).build();
        
		return new PolyglotQueryRequest(new PolyglotQueryRequestHandler() {
			@Override
			public void execute(Value value) {
				Object mappedObject = ValueMapper.convertValue(value);System.out.println(mappedObject);
				
				handler.execute(mappedObject, graphQL);
			}});
	}
	
	public void preStart(PolyglotContext contextPolyglot) {
		contextPolyglot.stateStore().put("result", 41);
		contextPolyglot.streams().subscribe("MyTopic_in");
	}

	@Override
	public CharSequence script() {
		return """
			function execute(api, message, state, queryReq, streams) {
				api.info("welcome");
				api.info(message.value());
				
				api.info(state.get("result"));
				state.put("result", state.get("result")+1);
				streams.publish("MyTopic_out", state.get("result")+1);
				
				const id = state.get("result");
				const gql = (query, variables = {}) => queryReq.execute({ query, variables });
				
				const myUserObject = {
					id,
					name: "Alice Smith",
					email: "alice@example.com"
				};
				
				gql(`mutation ($user: CreateUserInput!) { createUser(user: $user) { id } }`, { user: myUserObject });
				
				gql(`query ($id: Int!) { getUser(id: $id) { id name email } }`, { id });
			}
		""";
	}
}
