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
package io.actor4j.agents.langchain4j.pods.features;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.actor4j.agents.langchain4j.pods.ChatModelConfig;
import io.actor4j.agents.langchain4j.pods.ChatModelPod;
import io.actor4j.agents.langchain4j.pods.ChatModelResourceActor;
import io.actor4j.core.ActorRuntime;
import io.actor4j.core.ActorSystem;
import io.actor4j.core.actors.Actor;
import io.actor4j.core.config.ActorSystemConfig;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.pods.PodConfiguration;
import io.actor4j.core.pods.utils.PodStatus;

import static org.junit.Assert.*;

// a failed model call (no model needed) is answered with INTERNAL_SERVER_ERROR instead of staying pending
public class AgentFailureFeature {
	protected ActorSystem system;

	@Before
	public void before() {
		system = ActorSystem.create(ActorRuntime.factory(), ActorSystemConfig.builder().serverMode().parallelism(4).build());
	}

	public static class FailingChatModelPod extends ChatModelPod {
		public FailingChatModelPod(String domain, ChatModelConfig chatModelConfig) {
			super(domain, chatModelConfig);
		}

		@Override
		public ChatModelResourceActor createChatModelResourceActor(ChatModelConfig chatModelConfig) {
			return new ChatModelResourceActor(chatModelConfig) {
				@Override
				public ChatModel createAiChatModel(ChatModelConfig chatModelConfig) {
					return new ChatModel() {
						@Override
						public ChatResponse doChat(ChatRequest chatRequest) {
							throw new IllegalStateException("Model unavailable");
						}
					};
				}

				@Override
				public void receive(ActorMessage<?> message) {
					try {
						reply(message, chat(null, message.valueAsString()));
					}
					catch (Exception e) {
						fail(message, e);
					}
				}
			};
		}
	}

	@Test(timeout=10000)
	public void test_failing_model_call() {
		CountDownLatch testDone = new CountDownLatch(1);
		AtomicReference<ActorMessage<?>> received = new AtomicReference<>();

		String domain = "FailingChatModelPod";
		system.deployPods(
				() -> new FailingChatModelPod(domain, ChatModelConfig.create().build()),
				new PodConfiguration(domain, FailingChatModelPod.class.getName(), 1, 1));
		ActorId client = system.addActor(() -> new Actor(){
			@Override
			public void receive(ActorMessage<?> message) {
				received.set(message);
				testDone.countDown();
			}
		});
		system.start();

		system.sendViaAlias(ActorMessage.create("Hello", 0, client, null), domain);

		try {
			testDone.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		system.shutdownWithActors(true);

		assertEquals(PodStatus.INTERNAL_SERVER_ERROR, received.get().tag());
		assertTrue(received.get().value() instanceof IllegalStateException);
		assertEquals("Model unavailable", ((Throwable)received.get().value()).getMessage());
	}
}
