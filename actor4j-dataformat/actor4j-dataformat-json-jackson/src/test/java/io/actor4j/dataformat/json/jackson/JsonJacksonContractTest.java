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
package io.actor4j.dataformat.json.jackson;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import io.actor4j.core.json.JsonArray;
import io.actor4j.core.json.JsonObject;

// regression tests for the review 2026-10 (N1, matrix M16), contract as in the other dataformat modules
public class JsonJacksonContractTest {
	@Test(expected=ClassCastException.class)
	public void test_wrong_type_object_in_array() {
		// N1: thrown, no stacktrace and null
		JsonArray.create("[1]").getJsonObject(0);
	}

	@Test(expected=ClassCastException.class)
	public void test_wrong_type_object() {
		JsonObject.create("{\"k\":1}").getJsonObject("k");
	}

	@Test
	public void test_array_from_object() {
		// M16
		assertEquals(2, JsonArray.create((Object)List.of(1, 2)).size());
		assertEquals(3, JsonArray.create((Object)new int[] {1, 2, 3}).size());
	}

	@Test(expected=IllegalArgumentException.class)
	public void test_array_from_unsupported_type() {
		JsonArray.create((Object)"no array");
	}

	@Test(expected=IllegalArgumentException.class)
	public void test_object_from_unsupported_type() {
		JsonObject.create((Object)"no object");
	}

	@Test
	public void test_mergeIn_copies_nested_values() {
		JsonObject source = JsonObject.create().put("n", JsonObject.create().put("k", 1));
		JsonObject target = JsonObject.create().mergeIn(source);
		source.getJsonObject("n").put("k", 2);

		assertEquals(Integer.valueOf(1), target.getJsonObject("n").getInteger("k"));
	}

	@Test
	public void test_foreign_implementation_by_content() {
		// a JsonObject of another implementation is taken by its content, not as a bean
		JsonObject foreign = new ForeignJsonObject("{\"k\":\"v\",\"n\":[1,2]}");

		assertEquals(JsonObject.create("{\"k\":\"v\",\"n\":[1,2]}"), JsonObject.create().mergeIn(foreign));
		assertEquals("{\"f\":{\"k\":\"v\",\"n\":[1,2]}}", JsonObject.create().put("f", foreign).encode());
	}

	@Test
	public void test_numbers_by_value() {
		JsonObject a = JsonObject.create("{\"x\":1,\"n\":{\"k\":[1,2.5]}}");
		JsonObject b = JsonObject.create().put("n", JsonObject.create("{\"k\":[1,2.50]}")).put("x", 1L);

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertEquals(JsonObject.create("{\"x\":1}"), JsonObject.create("{\"x\":1.0}"));
		assertNotEquals(a, JsonObject.create("{\"x\":2,\"n\":{\"k\":[1,2.5]}}"));
		assertTrue(JsonArray.create("[1]").contains(1L));
	}

	@Test
	public void test_iterator_remove() {
		JsonArray array = JsonArray.create("[1,2,3]");
		Iterator<Object> iterator = array.iterator();
		iterator.next();
		iterator.remove();

		assertEquals(JsonArray.create("[2,3]"), array);
	}

	// a JsonObject that is not a JsonObjectImpl, delegating to one
	protected static class ForeignJsonObject extends JsonObjectImpl {
		public ForeignJsonObject(String json) {
			super(json);
		}
	}
}
