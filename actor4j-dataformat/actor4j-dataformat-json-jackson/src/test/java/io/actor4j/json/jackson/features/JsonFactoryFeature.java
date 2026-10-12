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
package io.actor4j.json.jackson.features;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import io.actor4j.core.json.JsonArray;
import io.actor4j.core.json.JsonObject;
import io.actor4j.dataformat.json.jackson.JsonArrayImpl;
import io.actor4j.dataformat.json.jackson.JsonObjectImpl;

public class JsonFactoryFeature {
	record Plan(String summary, List<String> steps) {}

	@Test
	public void test_service_loader() {
		assertTrue(JsonObject.create() instanceof JsonObjectImpl);
		assertTrue(JsonArray.create() instanceof JsonArrayImpl);
	}

	@Test
	public void test_parse_and_get() {
		JsonObject obj = JsonObject.create("""
			{ "text": "hello", "int": 42, "long": 12345678901, "double": 0.8, "bool": true, "none": null,
			  "obj": { "a": 1 }, "arr": [1, "two", { "b": 3 }] }""");

		assertEquals("hello", obj.getString("text"));
		assertEquals(Integer.valueOf(42), obj.getInteger("int"));
		assertEquals(Long.valueOf(12345678901L), obj.getLong("long"));
		assertEquals(Double.valueOf(0.8), obj.getDouble("double"));
		assertEquals(Boolean.TRUE, obj.getBoolean("bool"));
		assertNull(obj.getString("none"));
		assertNull(obj.getString("missing"));
		assertTrue(obj.containsKey("none"));
		assertFalse(obj.containsKey("missing"));

		assertEquals(Integer.valueOf(1), obj.getJsonObject("obj").getInteger("a"));
		JsonArray arr = obj.getJsonArray("arr");
		assertEquals(3, arr.size());
		assertEquals(Integer.valueOf(1), arr.getInteger(0));
		assertEquals("two", arr.getString(1));
		assertEquals(Integer.valueOf(3), arr.getJsonObject(2).getInteger("b"));

		assertTrue(obj.getValue("obj") instanceof JsonObject);
		assertTrue(obj.getValue("arr") instanceof JsonArray);
		assertEquals(List.of("text", "int", "long", "double", "bool", "none", "obj", "arr"), new ArrayList<>(obj.fieldNames()));
	}

	@Test
	public void test_wrong_type() {
		JsonObject obj = JsonObject.create("{ \"text\": \"hello\", \"obj\": { } }");

		assertThrows(ClassCastException.class, () -> obj.getInteger("text"));
		assertThrows(ClassCastException.class, () -> obj.getString("obj"));
		assertNull(obj.getJsonArray("obj"));
	}

	@Test
	public void test_invalid_json() {
		assertThrows(IllegalArgumentException.class, () -> JsonObject.create("{ not json"));
		assertThrows(IllegalArgumentException.class, () -> JsonObject.create("[1, 2]"));
		assertThrows(IllegalArgumentException.class, () -> JsonArray.create("{ }"));
	}

	@Test
	public void test_put_nested() {
		JsonObject inner = JsonObject.create().put("a", 1);
		JsonArray list = JsonArray.create().add("x").add(inner);
		JsonObject obj = JsonObject.create()
			.put("inner", inner)
			.put("list", list)
			.put("map", Map.of("k", "v"))
			.put("none", null);

		assertEquals("{\"inner\":{\"a\":1},\"list\":[\"x\",{\"a\":1}],\"map\":{\"k\":\"v\"},\"none\":null}", obj.encode());
		assertEquals(2, list.size()); // added once, not twice
		assertTrue(list.contains("x"));
		assertTrue(list.contains(JsonObject.create("{\"a\":1}")));

		// sharing: changes of a nested object are visible in the parent (like Vert.x)
		obj.getJsonObject("inner").put("b", 2);
		assertEquals(Integer.valueOf(2), obj.getJsonObject("inner").getInteger("b"));
	}

	@Test
	public void test_pojo_and_record() {
		JsonObject obj = JsonObject.create(new Plan("Backups", List.of("Schedule", "Storage")));

		assertEquals("Backups", obj.getString("summary"));
		assertEquals("Storage", obj.getJsonArray("steps").getString(1));
		assertEquals("{\"summary\":\"Backups\",\"steps\":[\"Schedule\",\"Storage\"]}", JsonObject.mapFrom(new Plan("Backups", List.of("Schedule", "Storage"))));
	}

	@Test
	public void test_array() {
		JsonArray arr = JsonArray.create(List.of(1, "two", Map.of("three", 3)));
		arr.add(0, "zero").set(1, 10);

		assertEquals("[\"zero\",10,\"two\",{\"three\":3}]", arr.encode());
		assertEquals("zero", arr.remove(0));
		assertEquals(List.of(10, "two", Map.of("three", 3)), arr.getList());

		List<Object> values = new ArrayList<>();
		for (Object value : arr)
			values.add(value);
		assertEquals(10, values.get(0));
		assertTrue(values.get(2) instanceof JsonObject);

		JsonArray other = JsonArray.create().addAll(arr);
		assertEquals(arr, other);
		assertTrue(arr.clear().isEmpty());
	}

	@Test
	public void test_merge_map_remove() {
		JsonObject obj = JsonObject.create("{ \"a\": 1, \"b\": { \"c\": [1, 2] } }");
		obj.mergeIn(JsonObject.create("{ \"a\": 2, \"d\": true }"));

		assertEquals(Integer.valueOf(2), obj.getInteger("a"));
		assertEquals(Boolean.TRUE, obj.getBoolean("d"));
		assertEquals(Map.of("a", 2, "b", Map.of("c", List.of(1, 2)), "d", true), obj.getMap());
		assertEquals(Integer.valueOf(2), obj.remove("a"));
		assertEquals(2, obj.size());
		assertTrue(obj.clear().isEmpty());
	}
}
