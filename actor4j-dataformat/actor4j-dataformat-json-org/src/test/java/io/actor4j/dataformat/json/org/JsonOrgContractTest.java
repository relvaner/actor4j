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
package io.actor4j.dataformat.json.org;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import io.actor4j.core.json.JsonArray;
import io.actor4j.core.json.JsonObject;

// contract as json-jackson, regression tests for the review 2026-10 (H1, M1 to M4)
public class JsonOrgContractTest {
	public record Plan(String name, int count) {
	}

	public static class Bean {
		public String getName() {
			return "bean";
		}
	}

	@Test
	public void test_service_loader() {
		assertTrue(JsonObject.create() instanceof JsonObjectImpl);
		assertTrue(JsonArray.create() instanceof JsonArrayImpl);
	}

	// H1

	@Test
	public void test_object_from_map() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("k", "v");
		map.put("n", Map.of("x", 1));
		JsonObject object = JsonObject.create((Object)map);

		assertEquals("v", object.getString("k"));
		assertEquals(Integer.valueOf(1), object.getJsonObject("n").getInteger("x"));
	}

	@Test
	public void test_object_from_record() {
		JsonObject object = JsonObject.create((Object)new Plan("p", 2));

		assertEquals("p", object.getString("name"));
		assertEquals(Integer.valueOf(2), object.getInteger("count"));
	}

	@Test
	public void test_object_from_bean() {
		assertEquals("bean", JsonObject.create((Object)new Bean()).getString("name"));
	}

	@Test(expected=IllegalArgumentException.class)
	public void test_object_from_unsupported_type() {
		JsonObject.create((Object)42);
	}

	@Test
	public void test_array_from_list_and_array() {
		assertEquals(2, JsonArray.create((Object)List.of(1, 2)).size());
		assertEquals(3, JsonArray.create((Object)new int[] {1, 2, 3}).size());

		JsonArray array = JsonArray.create(List.of(new Plan("a", 1), Map.of("k", "v")));
		assertEquals("a", array.getJsonObject(0).getString("name"));
		assertEquals("v", array.getJsonObject(1).getString("k"));
	}

	@Test(expected=IllegalArgumentException.class)
	public void test_array_from_unsupported_type() {
		JsonArray.create((Object)"no array");
	}

	// M1

	@Test
	public void test_missing_key_is_null() {
		JsonObject object = JsonObject.create();

		assertNull(object.getString("missing"));
		assertNull(object.getInteger("missing"));
		assertNull(object.getJsonObject("missing"));
		assertNull(object.getValue("missing"));
		assertNull(JsonArray.create().getString(5));
	}

	@Test(expected=ClassCastException.class)
	public void test_wrong_type_number() {
		JsonObject.create("{\"k\":\"text\"}").getInteger("k");
	}

	@Test(expected=ClassCastException.class)
	public void test_wrong_type_object() {
		JsonObject.create("{\"k\":1}").getJsonObject("k");
	}

	@Test
	public void test_simple_value_as_string() {
		assertEquals("1", JsonObject.create("{\"k\":1}").getString("k")); // as jackson
	}

	@Test(expected=IllegalArgumentException.class)
	public void test_invalid_json_object() {
		JsonObject.create("{ not json");
	}

	@Test(expected=IllegalArgumentException.class)
	public void test_invalid_json_array() {
		JsonArray.create("[ not json");
	}

	// M2

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
		assertTrue(array.contains(1L)); // numbers by value
		assertTrue(array.contains("s"));
		assertFalse(array.contains("x"));
	}

	// M3

	@Test
	public void test_put_null_keeps_the_key() {
		JsonObject object = JsonObject.create().put("k", null);

		assertTrue(object.containsKey("k"));
		assertNull(object.getValue("k"));
		assertEquals("{\"k\":null}", object.encode());
	}

	@Test(expected=NullPointerException.class)
	public void test_put_null_key() {
		JsonObject.create().put(null, "v");
	}

	// M4

	@Test
	public void test_getValue_wraps_nested_values() {
		JsonObject object = JsonObject.create("{\"o\":{\"k\":1},\"a\":[1],\"n\":null}");

		assertTrue(object.getValue("o") instanceof JsonObject);
		assertTrue(object.getValue("a") instanceof JsonArray);
		assertNull(object.getValue("n"));
		assertTrue(object.remove("o") instanceof JsonObject);
	}

	@Test
	public void test_iterator_wraps_nested_values() {
		JsonArray array = JsonArray.create("[{\"k\":1},[2],null,\"s\"]");
		List<Object> values = new ArrayList<>();
		for (Object value : array)
			values.add(value);

		assertTrue(values.get(0) instanceof JsonObject);
		assertTrue(values.get(1) instanceof JsonArray);
		assertNull(values.get(2));
		assertEquals("s", values.get(3));
		assertTrue(array.getValue(0) instanceof JsonObject);
	}

	@Test
	public void test_iterator_remove() {
		JsonArray array = JsonArray.create("[1,2]");
		Iterator<Object> iterator = array.iterator();
		iterator.next();
		iterator.remove();

		assertEquals("[2]", array.encode());
	}

	@Test
	public void test_mergeIn_copies_nested_values() {
		JsonObject source = JsonObject.create().put("n", JsonObject.create().put("k", 1));
		JsonObject target = JsonObject.create().mergeIn(source);
		source.getJsonObject("n").put("k", 2);

		assertEquals(Integer.valueOf(1), target.getJsonObject("n").getInteger("k"));
	}

	// add(pos) inserts (JSONArray.put(pos) replaces)

	@Test
	public void test_add_at_position_inserts() {
		JsonArray array = JsonArray.create("[1,2,3]");
		array.add(1, 9);
		array.add(10, 4); // beyond the end: appended

		assertEquals("[1,9,2,3,4]", array.encode());
	}

	@Test
	public void test_set_replaces() {
		assertEquals("[1,9,3]", JsonArray.create("[1,2,3]").set(1, 9).encode());
	}

	// N3

	@Test
	public void test_equals_and_hashCode_object() {
		JsonObject a = JsonObject.create("{\"x\":1,\"n\":{\"k\":[1,2]},\"s\":\"v\"}");
		JsonObject b = JsonObject.create().put("s", "v").put("n", JsonObject.create("{\"k\":[1,2]}")).put("x", 1L); // other order, Long

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, JsonObject.create("{\"x\":2,\"n\":{\"k\":[1,2]},\"s\":\"v\"}"));
		assertNotEquals(a, JsonObject.create("{\"x\":1}"));
	}

	@Test
	public void test_equals_and_hashCode_array() {
		JsonArray a = JsonArray.create("[1,{\"k\":\"v\"},null]");
		JsonArray b = JsonArray.create().add(1L).add(JsonObject.create().put("k", "v")).add(null);

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, JsonArray.create("[{\"k\":\"v\"},1,null]")); // order matters
	}
}
