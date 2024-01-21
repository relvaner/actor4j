/*
 * Copyright (c) 2015-2024, David A. Bauer. All rights reserved.
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
package io.actor4j.apc.channels.features;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.actor4j.apc.channels.APC;
import io.actor4j.apc.channels.Channel;
import io.actor4j.core.ActorRuntime;

public class APCFeature {
	@Test
	public void test() {
		APC apc = APC.create(ActorRuntime.create());
		apc.start();
		
		Channel<String> channel = apc.createChannel();
		
		channel.offer("Message:");
		apc.fork((ch) -> ch.offer("Hello"), channel);
		apc.fork((ch) -> ch.offer(" World!"), channel);

		String result1 = apc.take(channel);
		System.out.println(result1);
		assertTrue(result1.equals("Message:"));
		
		String result2 = apc.take(channel, 2).stream().reduce("", (s1, s2) -> s1+s2);
		System.out.println(result2);
		assertTrue(result2.equals("Hello World!") || result2.equals(" World!Hello"));
		
		apc.shutdown();
	}
}
