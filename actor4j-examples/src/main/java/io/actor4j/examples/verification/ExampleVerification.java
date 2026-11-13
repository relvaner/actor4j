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
package io.actor4j.examples.verification;

import static io.actor4j.core.logging.ActorLogger.*;

import io.actor4j.core.actors.Actor;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.verification.ActorVerification;
import io.actor4j.verification.ActorVerificationSM;
import io.actor4j.verification.ActorVerifier;
import io.actor4j.verification.VerificationResult;

public class ExampleVerification {
	public static final int PING = 100;
	public static final int PONG = 101;

	protected static class Ping extends Actor implements ActorVerification {
		public Ping(String name) {
			super(name);
		}

		@Override
		public void receive(ActorMessage<?> message) {
			tell("ping", PING, message.source());
		}

		@Override
		public ActorVerificationSM verify() {
			return ActorVerificationSM.of(this)
				.initial("PING")
				.on(PONG).from("PING").to("PING").send(PING, "pong")
				.on(0).from("PING").to("D")
				.on(0).from("A").to("B")
				.on(0).from("B").to("C")
				.on(0).from("C").to("A");
		}
	}

	protected static class Pong extends Actor implements ActorVerification {
		public Pong(String name) {
			super(name);
		}

		@Override
		public void receive(ActorMessage<?> message) {
			tell("pong", PONG, message.source());
		}

		@Override
		public ActorVerificationSM verify() {
			return ActorVerificationSM.of(this)
				.initial("PONG")
				.on(PING).from("PONG").to("PONG").send(PONG, "ping");
		}
	}

	public static void main(String[] args) {
		VerificationResult result = ActorVerifier.verify(new Ping("ping"), new Pong("pong"));

		logger().log(INFO, String.format("ping - Cycles: %s", result.actor("ping").cycles()));
		logger().log(INFO, String.format("ping - Unreachables: %s", result.actor("ping").unreachables()));
		logger().log(INFO, String.format("ping - Deads: %s", result.actor("ping").dead()));

		logger().log(INFO, String.format("All - Cycles: %s", result.all().cycles()));
		logger().log(INFO, String.format("All - Unreachables: %s", result.all().from("ping").unreachables()));
		logger().log(INFO, String.format("All - Deads: %s", result.all().from("ping").dead()));

		logger().log(INFO, "\n"+result.report());
	}
}
