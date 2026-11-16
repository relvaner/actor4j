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
package io.actor4j.dataformat.msgpack.jackson;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.junit.Test;

import io.actor4j.core.json.api.ObjectMapperService;
import io.actor4j.core.utils.GenericType;
import io.actor4j.dataformat.msgpack.jackson.spi.SerializerProvider;

// regression tests for the review 2026-10 (H1, M1)
public class MsgPackSerializerTest {
	public record Plan(String name, int count, List<String> steps) {
	}

	protected final SerializerProvider serializer = new SerializerProvider();

	// H1

	@Test
	public void test_no_object_mapper_service() {
		for (ObjectMapperService service : ServiceLoader.load(ObjectMapperService.class))
			fail("unexpected ObjectMapperService: "+service.getClass().getName());
	}

	// roundtrip

	@Test
	public void test_roundtrip_record() {
		Plan plan = new Plan("p", 2, List.of("a", "b"));

		assertEquals(plan, serializer.decode(serializer.encode(plan), Plan.class));
	}

	@Test
	public void test_roundtrip_generic_type() {
		List<Integer> list = List.of(1, 2);

		assertEquals(list, serializer.decode(serializer.encode(list), new GenericType<List<Integer>>() {}));
	}

	@Test
	public void test_unknown_properties_are_ignored() {
		byte[] src = serializer.encode(Map.of("name", "p", "count", 2, "extra", true));

		assertEquals("p", serializer.decode(src, Plan.class).name());
	}

	// M1

	@Test
	public void test_decode_without_type() {
		Object map = serializer.decode(serializer.encode(Map.of("k", List.of(1, "two"))));

		assertEquals(Map.of("k", List.of(1, "two")), map);
	}

	@Test(expected=IllegalArgumentException.class)
	public void test_invalid_bytes() {
		serializer.decode(new byte[] {(byte)0xc1}, List.class);
	}

	@Test(expected=IllegalArgumentException.class)
	public void test_invalid_bytes_without_type() {
		serializer.decode(new byte[] {(byte)0xc1});
	}

	@Test(expected=IllegalArgumentException.class)
	public void test_wrong_target_type() {
		serializer.decode(serializer.encode("text"), Plan.class);
	}

	@Test(expected=IllegalArgumentException.class)
	public void test_not_serializable() {
		serializer.encode(new Object()); // no properties
	}
}
