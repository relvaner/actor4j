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
package io.actor4j.verification.features;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import io.actor4j.core.actors.Actor;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.verification.*;

public final class VerificationTestUtils {
	public static final int PING = 100;
	public static final int PONG = 101;

	private VerificationTestUtils() {
	}

	// state machine of an actor that is not part of an actor system
	public static ActorVerificationSM sm(String name) {
		return new ActorVerificationSM(new Actor(name) {
			@Override
			public void receive(ActorMessage<?> message) {
				// not used
			}
		});
	}

	// example of the documentation
	public static ActorVerificationSM ping() {
		return sm("ping")
			.addInitialStateMarker("PING")
			.addInTransition("PING", "PING", PONG)
			.addOutTransition("PING", "PING", PING, "pong")
			.addStateMarker("D")
			.addInTransition("PING", "D", 0)
			.addStateMarker("A")
			.addStateMarker("B")
			.addStateMarker("C")
			.addInTransition("A", "B", 0)
			.addInTransition("B", "C", 0)
			.addInTransition("C", "A", 0);
	}

	public static ActorVerificationSM pong() {
		return sm("pong")
			.addInitialStateMarker("PONG")
			.addInTransition("PONG", "PONG", PING)
			.addOutTransition("PONG", "PONG", PONG, "ping");
	}

	// cycles independent of the start vertex and the order found
	public static <V> Set<Set<V>> normalize(List<List<V>> cycles) {
		Set<Set<V>> result = new HashSet<>();
		for (List<V> cycle : cycles)
			result.add(new HashSet<>(cycle));
		return result;
	}

	public static class VerifiableActor extends Actor implements ActorVerification {
		protected final Supplier<ActorVerificationSM> model;

		public VerifiableActor(String name, Supplier<ActorVerificationSM> model) {
			super(name);
			this.model = model;
		}

		@Override
		public void receive(ActorMessage<?> message) {
			// not used
		}

		@Override
		public ActorVerificationSM verify() {
			return model.get();
		}
	}
}
