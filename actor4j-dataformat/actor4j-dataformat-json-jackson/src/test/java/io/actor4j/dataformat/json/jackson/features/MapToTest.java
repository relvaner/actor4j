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
package io.actor4j.dataformat.json.jackson.features;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import io.actor4j.core.json.JsonArray;
import io.actor4j.core.json.JsonObject;

// JsonObject.mapTo and JsonArray.mapTo (default methods over the ObjectMapper)
public class MapToTest {
	public record Address(String city, int zip) {}
	public record Person(String name, int age, Address address, List<String> tags) {}
	public enum Color { RED, GREEN }

	@Test
	public void test_object_to_record() {
		Person person = new Person("Ann", 30, new Address("Berlin", 10115), List.of("a", "b"));

		assertEquals(person, JsonObject.create(person).mapTo(Person.class));
	}

	@Test
	public void test_object_from_json() {
		JsonObject json = JsonObject.create("{\"name\":\"Bob\",\"age\":42,\"address\":{\"city\":\"Rome\",\"zip\":100},\"tags\":[]}");

		assertEquals(new Person("Bob", 42, new Address("Rome", 100), List.of()), json.mapTo(Person.class));
	}

	@Test(expected=IllegalArgumentException.class)
	public void test_object_invalid_content() {
		JsonObject.create("{\"name\":\"Bob\",\"age\":\"old\"}").mapTo(Person.class);
	}

	@Test
	public void test_array_of_objects() {
		List<Address> addresses = List.of(new Address("Berlin", 10115), new Address("Rome", 100));

		assertEquals(addresses, JsonArray.create(addresses).mapTo(Address.class));
	}

	@Test
	public void test_array_of_simple_values() {
		JsonArray json = JsonArray.create("[1, 2, null, 3]");

		assertEquals(Arrays.asList(1L, 2L, null, 3L), json.mapTo(Long.class)); // converted
		assertEquals(Arrays.asList(1, 2, null, 3), json.mapTo(Integer.class)); // unchanged
		assertEquals(List.of(Color.RED, Color.GREEN), JsonArray.create("[\"RED\", \"GREEN\"]").mapTo(Color.class));
		assertEquals(List.of("x", "y"), JsonArray.create("[\"x\", \"y\"]").mapTo(String.class));
	}

	@Test
	public void test_array_of_arrays() {
		List<int[]> result = JsonArray.create("[[1, 2], [3]]").mapTo(int[].class);

		assertArrayEquals(new int[] {1, 2}, result.get(0));
		assertArrayEquals(new int[] {3}, result.get(1));
	}
}
