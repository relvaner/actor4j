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

import static io.actor4j.verification.features.VerificationTestUtils.*;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.actor4j.core.actors.Actor;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.verification.*;

// conformance check: messages of a running system compared with the models of the actors
public class ConformanceFeature {
	public static final int OTHER = 102;

	// plays a fixed number of rounds, then stops replying
	protected static class Ping extends Actor implements ActorVerification, ActorVerificationState {
		protected final CountDownLatch done;
		protected int rounds;
		protected String state = "PING";

		public Ping(String name, CountDownLatch done) {
			super(name);
			this.done = done;
		}

		@Override
		public void receive(ActorMessage<?> message) {
			if (message.tag()==PONG) {
				if (++rounds<=5)
					tell(null, PING, message.source());
				else
					done.countDown();
			}
		}

		@Override
		public String state() {
			return state;
		}

		@Override
		public ActorVerificationSM verify() {
			return ActorVerificationSM.of(this)
				.initial("PING")
				.on(PONG).from("PING").to("PING").send(PING, "pong");
		}
	}

	protected static class Pong extends Actor implements ActorVerification {
		protected final int reply;

		public Pong(String name, int reply) {
			super(name);
			this.reply = reply;
		}

		@Override
		public void receive(ActorMessage<?> message) {
			tell(null, reply, message.source());
		}

		@Override
		public ActorVerificationSM verify() {
			return ActorVerificationSM.of(this)
				.initial("PONG")
				.on(PING).from("PONG").to("PONG").send(PONG, "ping");
		}
	}

	// runs ping-pong, pong replies with the given tag
	protected ConformanceResult run(int reply, Map<Integer, String> eventNames) throws InterruptedException {
		CountDownLatch done = new CountDownLatch(1);
		ActorVerificator verificator = ActorVerificator.create();
		ActorId ping = verificator.addActor(() -> new Ping("ping", done));
		ActorId pong = verificator.addActor(() -> new Pong("pong", reply));
		verificator.start();

		verificator.send(ActorMessage.create(null, PONG, pong, ping)); // as if pong had sent it
		done.await(reply==PONG ? 2000 : 300, TimeUnit.MILLISECONDS); // without PONG ping stops after the first round
		verificator.shutdownWithActors(true);

		return verificator.conformance(eventNames);
	}

	@Test(timeout=5000)
	public void test_ping_pong_conforms() throws InterruptedException {
		ConformanceResult result = run(PONG, Map.of());

		assertTrue(result.toString(), result.conforms());
		assertTrue(result.observed()>=10);
		VerificationAssert.assertConforms(result);
	}

	@Test(timeout=5000)
	public void test_message_not_in_the_model() throws InterruptedException {
		// pong replies with OTHER: neither pong may send it nor ping receive it
		ConformanceResult result = run(OTHER, Map.of(OTHER, "OTHER"));

		assertFalse(result.conforms());
		assertEquals(List.of(
			"pong sent OTHER to ping, not in the model",
			"ping received OTHER from pong, not in the model"), result.violations());
		try {
			VerificationAssert.assertConforms(result);
			fail();
		}
		catch (AssertionError e) {
			assertTrue(e.getMessage().startsWith("not conform to the models:"));
		}
	}

	@Test(timeout=5000)
	public void test_sender_state_is_checked() throws InterruptedException {
		// ping is in state WAIT, its model only sends PING in state PING
		CountDownLatch done = new CountDownLatch(1);
		ActorVerificator verificator = ActorVerificator.create();
		ActorId ping = verificator.addActor(() -> new Ping("ping", done) {
			@Override
			public String state() {
				return "WAIT";
			}
		});
		ActorId pong = verificator.addActor(() -> new Pong("pong", PONG));
		verificator.start();

		verificator.send(ActorMessage.create(null, PONG, pong, ping));
		done.await(2, TimeUnit.SECONDS);
		verificator.shutdownWithActors(true);

		assertEquals(List.of("ping sent 100 to pong in state WAIT, not in the model"), verificator.conformance().violations());
	}

	@Test
	public void test_traps() {
		// N4: pong can never leave PONG, ping has a way out (D is dead, not a trap)
		VerificationResult result = ActorVerifier.verify(new VerifiableActor("ping", VerificationTestUtils::ping), new VerifiableActor("pong", VerificationTestUtils::pong));

		assertEquals(java.util.Set.of("pong:PONG"), result.actor("pong").traps());
		assertTrue(result.actor("ping").traps().isEmpty());
		assertEquals(java.util.Set.of("ping:D"), result.actor("ping").dead());
	}
}
