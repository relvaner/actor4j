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
package io.actor4j.dataformat.json.vertx;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import io.actor4j.core.json.JsonArray;
import io.actor4j.core.json.JsonObject;

// contract as json-jackson, regression tests for the review 2026-10 (H1, M1, M2, N1 to N3)
public class JsonVertxContractTest {
	public record Plan(String name, int count) {
	}

	@Test
	public void test_service_loader() {
		assertTrue(JsonObject.create() instanceof JsonObjectImpl);
		assertTrue(JsonArray.create() instanceof JsonArrayImpl);
	}

	// creation (H1, M1, M2)

	@Test
	public void test_object_from_map() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("k", "v");
		map.put("n", Map.of("x", 1));
		JsonObject object = JsonObject.create((Object)map);

		assertEquals("v", object.getString("k"));
		assertEquals(Integer.valueOf(1), object.getJsonObject("n").getInteger("x"));
		object.put("y", 2); // the map is copied, Map.of stays untouched
	}

	@Test(expected=IllegalArgumentException.class)
	public void test_object_from_map_with_non_string_key() {
		JsonObject.create((Object)Map.of(1, "v"));
	}

	@Test
	public void test_object_from_record_needs_databind() {
		// vertx-core without jackson-databind: a clear IllegalStateException instead of a DecodeException
		try {
			JsonObject object = JsonObject.create((Object)new Plan("p", 2));
			assertEquals("p", object.getString("name")); // with jackson-databind on the classpath
		}
		catch (IllegalStateException e) {
			assertTrue(e.getMessage().contains("jackson-databind"));
		}
	}

	@Test
	public void test_array_from_list_and_array() {
		assertEquals(2, JsonArray.create((Object)List.of(1, 2)).size());
		assertEquals(3, JsonArray.create((Object)new int[] {1, 2, 3}).size());
		assertEquals("[1,2,3]", JsonArray.create(List.of(1, 2)).add(3).encode()); // the list is copied
	}

	@Test(expected=IllegalArgumentException.class)
	public void test_array_from_unsupported_type() {
		JsonArray.create((Object)"no array");
	}

	@Test
	public void test_nested_values_are_encoded_once() {
		JsonObject inner = JsonObject.create().put("x", 1);

		assertEquals("{\"inner\":{\"x\":1}}", JsonObject.create().put("inner", inner).encode());
		assertEquals("[{\"x\":1}]", JsonArray.create().add(inner).encode());
	}

	// null, missing values, wrong types, invalid JSON

	@Test
	public void test_missing_key_is_null() {
		JsonObject object = JsonObject.create();

		assertNull(object.getString("missing"));
		assertNull(object.getInteger("missing"));
		assertNull(object.getJsonObject("missing"));
		assertNull(object.getValue("missing"));
	}

	@Test(expected=ClassCastException.class)
	public void test_wrong_type_number() {
		JsonObject.create("{\"k\":\"text\"}").getInteger("k");
	}

	@Test(expected=ClassCastException.class)
	public void test_wrong_type_object() {
		// N3: as the other getters, no stacktrace and null
		JsonObject.create("{\"k\":1}").getJsonObject("k");
	}

	@Test(expected=ClassCastException.class)
	public void test_wrong_type_array() {
		JsonArray.create("[1]").getJsonArray(0);
	}

	@Test(expected=IllegalArgumentException.class)
	public void test_invalid_json_object() {
		JsonObject.create("{ not json");
	}

	@Test(expected=IllegalArgumentException.class)
	public void test_invalid_json_array() {
		JsonArray.create("[ not json");
	}

	@Test
	public void test_put_null_keeps_the_key() {
		JsonObject object = JsonObject.create().put("k", null);

		assertTrue(object.containsKey("k"));
		assertNull(object.getValue("k"));
		assertEquals("{\"k\":null}", object.encode());
	}

	// N1: nested values wrapped

	@Test
	public void test_getValue_wraps_nested_values() {
		JsonObject object = JsonObject.create("{\"o\":{\"k\":1},\"a\":[1],\"n\":null}");

		assertTrue(object.getValue("o") instanceof JsonObjectImpl);
		assertTrue(object.getValue("a") instanceof JsonArrayImpl);
		assertNull(object.getValue("n"));
		assertTrue(object.remove("o") instanceof JsonObjectImpl);
	}

	@Test
	public void test_iterator_wraps_nested_values() {
		JsonArray array = JsonArray.create("[{\"k\":1},[2],null,\"s\"]");
		List<Object> values = new ArrayList<>();
		for (Object value : array)
			values.add(value);

		assertTrue(values.get(0) instanceof JsonObjectImpl);
		assertTrue(values.get(1) instanceof JsonArrayImpl);
		assertNull(values.get(2));
		assertEquals("s", values.get(3));
		assertTrue(array.getValue(0) instanceof JsonObjectImpl);
		assertTrue(array.remove(1) instanceof JsonArrayImpl);
	}

	@Test
	public void test_iterator_remove() {
		JsonArray array = JsonArray.create("[1,2]");
		Iterator<Object> iterator = array.iterator();
		iterator.next();
		iterator.remove();

		assertEquals("[2]", array.encode());
	}

	// addAll, contains, add(pos)

	@Test
	public void test_addAll_keeps_nested_objects() {
		JsonArray source = JsonArray.create().add(JsonObject.create().put("k", "v")).add(JsonArray.create().add(1));
		JsonArray target = JsonArray.create().addAll(source);

		assertEquals("v", target.getJsonObject(0).getString("k"));
		assertEquals(Integer.valueOf(1), target.getJsonArray(1).getInteger(0));
	}

	@Test
	public void test_contains() {
		JsonObject inner = JsonObject.create().put("k", "v");
		JsonArray array = JsonArray.create().add(inner).add(1).add("s");

		assertTrue(array.contains(inner));
		assertTrue(array.contains(JsonObject.create("{\"k\":\"v\"}"))); // equal content
		assertTrue(array.contains("s"));
		assertFalse(array.contains("x"));
	}

	@Test
	public void test_add_at_position_inserts() {
		assertEquals("[1,9,2,3]", JsonArray.create("[1,2,3]").add(1, 9).encode());
	}

	// N3: mergeIn

	@Test
	public void test_mergeIn_copies_nested_values() {
		JsonObject source = JsonObject.create().put("n", JsonObject.create().put("k", 1));
		JsonObject target = JsonObject.create().mergeIn(source);
		source.getJsonObject("n").put("k", 2);

		assertEquals(Integer.valueOf(1), target.getJsonObject("n").getInteger("k"));
	}

	// N2: toString, equals, hashCode

	@Test
	public void test_toString_is_json() {
		assertEquals("{\"k\":1}", JsonObject.create().put("k", 1).toString());
		assertEquals("[1]", JsonArray.create().add(1).toString());
	}

	@Test
	public void test_equals_and_hashCode() {
		JsonObject a = JsonObject.create("{\"x\":1,\"n\":{\"k\":[1,2]},\"s\":\"v\"}");
		JsonObject b = JsonObject.create().put("s", "v").put("n", JsonObject.create("{\"k\":[1,2]}")).put("x", 1L); // other order, Long

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, JsonObject.create("{\"x\":2,\"n\":{\"k\":[1,2]},\"s\":\"v\"}"));
		assertEquals(JsonArray.create("[1,{\"k\":\"v\"}]"), JsonArray.create().add(1L).add(JsonObject.create().put("k", "v")));
	}
}
