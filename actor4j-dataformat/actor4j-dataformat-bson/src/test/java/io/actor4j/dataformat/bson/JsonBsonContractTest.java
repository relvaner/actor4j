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
package io.actor4j.dataformat.bson;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.junit.Test;

import io.actor4j.core.json.JsonArray;
import io.actor4j.core.json.JsonObject;
import io.actor4j.dataformat.bson.spi.SerializerProvider;

// contract as json-jackson, regression tests for the review 2026-10 (H1 to H3, M1 to M3, N1 to N3)
public class JsonBsonContractTest {
	public record Plan(String name, int count) {
	}

	@Test
	public void test_service_loader() {
		assertTrue(JsonObject.create() instanceof JsonObjectImpl);
		assertTrue(JsonArray.create() instanceof JsonArrayImpl);
	}

	// H1

	@Test
	public void test_array_from_list() {
		JsonArray array = JsonArray.create(List.of(1, "two"));

		assertEquals(2, array.size());
		assertEquals("two", array.getString(1));
		array.add(3); // a copy, List.of stays untouched
		assertEquals(3, array.size());
	}

	@Test
	public void test_nested_array_is_shared() {
		JsonObject object = JsonObject.create("{\"arr\":[1,2]}");
		JsonArray array = object.getJsonArray("arr");
		array.add(3);

		assertEquals(3, object.getJsonArray("arr").size()); // changes arrive in the document
	}

	// H2

	@Test
	public void test_nested_values_are_stored_once() {
		JsonObject inner = JsonObject.create().put("x", 1);

		assertEquals(JsonObject.create("{\"inner\":{\"x\":1}}"), JsonObject.create().put("inner", inner));
		assertEquals(1, JsonArray.create().add(inner).size());
		assertEquals(JsonArray.create("[{\"x\":1}]"), JsonArray.create().add(inner));
	}

	// H3

	@Test
	public void test_array_encode_is_json() {
		JsonArray array = JsonArray.create().add("x").add(1).add(JsonObject.create().put("k", "v"));
		String json = array.encode();

		assertTrue(json, json.startsWith("[\"x\""));
		assertEquals(array, JsonArray.create(json)); // roundtrip
		assertEquals(array, JsonArray.create(array.encodePrettily()));
		assertTrue(array.encodePrettily().contains("\n"));
	}

	// M1

	@Test
	public void test_object_from_map_and_record() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("k", "v");
		map.put("n", Map.of("x", 1));
		JsonObject object = JsonObject.create((Object)map);

		assertEquals("v", object.getString("k"));
		assertEquals(Integer.valueOf(1), object.getJsonObject("n").getInteger("x"));
		assertEquals("p", JsonObject.create((Object)new Plan("p", 2)).getString("name"));
	}

	@Test(expected=IllegalArgumentException.class)
	public void test_object_from_unsupported_type() {
		JsonObject.create((Object)new StringBuilder("pojo")); // no codec for POJOs
	}

	@Test
	public void test_array_from_object_and_string() {
		assertEquals(2, JsonArray.create((Object)List.of(1, 2)).size());
		assertEquals(3, JsonArray.create((Object)new int[] {1, 2, 3}).size());
		assertEquals(Integer.valueOf(2), JsonArray.create("[1,2]").getInteger(1));
	}

	@Test(expected=IllegalArgumentException.class)
	public void test_array_from_unsupported_type() {
		JsonArray.create((Object)"no array");
	}

	@Test(expected=IllegalArgumentException.class)
	public void test_invalid_json_object() {
		JsonObject.create("{ not json");
	}

	@Test(expected=IllegalArgumentException.class)
	public void test_invalid_json_array() {
		JsonArray.create("[ not json");
	}

	@Test(expected=IllegalArgumentException.class)
	public void test_json_object_is_not_an_array() {
		JsonArray.create("{\"k\":1}");
	}

	// M2

	@Test
	public void test_numbers_and_strings_are_converted() {
		JsonObject object = JsonObject.create("{\"i\":42,\"d\":4.5}");

		assertEquals(Long.valueOf(42), object.getLong("i"));
		assertEquals(Integer.valueOf(4), object.getInteger("d"));
		assertEquals("42", object.getString("i"));
		assertEquals(Long.valueOf(42), JsonArray.create("[42]").getLong(0));
	}

	@Test
	public void test_missing_key_is_null() {
		JsonObject object = JsonObject.create();

		assertNull(object.getString("missing"));
		assertNull(object.getLong("missing"));
		assertNull(object.getJsonArray("missing"));
		assertNull(object.getValue("missing"));
	}

	@Test(expected=ClassCastException.class)
	public void test_wrong_type_number() {
		JsonObject.create("{\"k\":\"text\"}").getInteger("k");
	}

	@Test(expected=ClassCastException.class)
	public void test_wrong_type_object() {
		JsonObject.create("{\"k\":1}").getJsonObject("k");
	}

	@Test(expected=ClassCastException.class)
	public void test_wrong_type_object_in_array() {
		// N1: as with a key
		JsonArray.create("[1]").getJsonObject(0);
	}

	@Test
	public void test_put_null_keeps_the_key() {
		JsonObject object = JsonObject.create().put("k", null);

		assertTrue(object.containsKey("k"));
		assertNull(object.getValue("k"));
	}

	// nested values wrapped

	@Test
	public void test_getValue_and_iterator_wrap_nested_values() {
		JsonObject object = JsonObject.create("{\"o\":{\"k\":1},\"a\":[1]}");
		assertTrue(object.getValue("o") instanceof JsonObjectImpl);
		assertTrue(object.getValue("a") instanceof JsonArrayImpl);

		JsonArray array = JsonArray.create("[{\"k\":1},[2],\"s\"]");
		List<Object> values = new ArrayList<>();
		for (Object value : array)
			values.add(value);
		assertTrue(values.get(0) instanceof JsonObjectImpl);
		assertTrue(values.get(1) instanceof JsonArrayImpl);
		assertEquals("s", values.get(2));
	}

	@Test
	public void test_addAll_and_contains() {
		JsonObject inner = JsonObject.create().put("k", "v");
		JsonArray target = JsonArray.create().addAll(JsonArray.create().add(inner).add(1));

		assertEquals("v", target.getJsonObject(0).getString("k"));
		assertTrue(target.contains(JsonObject.create("{\"k\":\"v\"}")));
		assertTrue(target.contains(1L)); // numbers by value
		assertFalse(target.contains("x"));
	}

	@Test
	public void test_mergeIn_copies_nested_values() {
		JsonObject source = JsonObject.create().put("n", JsonObject.create().put("k", 1));
		JsonObject target = JsonObject.create().mergeIn(source);
		source.getJsonObject("n").put("k", 2);

		assertEquals(Integer.valueOf(1), target.getJsonObject("n").getInteger("k"));
	}

	// N2, N3

	@Test
	public void test_toString_encodePrettily_getMap_equals() {
		JsonObject object = JsonObject.create().put("b", 1).put("a", 2);

		assertEquals(object.encode(), object.toString());
		assertTrue(object.encodePrettily().contains("\n"));
		assertEquals(List.of("b", "a"), new ArrayList<>(object.getMap().keySet())); // order kept

		JsonObject a = JsonObject.create("{\"x\":1,\"n\":{\"k\":[1,2]}}");
		JsonObject b = JsonObject.create().put("n", JsonObject.create("{\"k\":[1,2]}")).put("x", 1L);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, JsonObject.create("{\"x\":2,\"n\":{\"k\":[1,2]}}"));
	}

	// M3

	@Test
	public void test_serializer_roundtrip() {
		SerializerProvider serializer = new SerializerProvider();
		JsonObject object = JsonObject.create("{\"k\":\"v\",\"n\":{\"a\":[1,2]}}");

		JsonObject result = serializer.decode(serializer.encode(object));
		assertEquals(object, result);
		assertEquals("v", serializer.decode(serializer.encode(object), Document.class) instanceof Document doc ? doc.getString("k") : null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void test_serializer_encodes_only_objects() {
		new SerializerProvider().encode("text");
	}

	@Test(expected=IllegalArgumentException.class)
	public void test_serializer_checks_the_target_type() {
		SerializerProvider serializer = new SerializerProvider();
		serializer.decode(serializer.encode(JsonObject.create()), String.class);
	}
}
