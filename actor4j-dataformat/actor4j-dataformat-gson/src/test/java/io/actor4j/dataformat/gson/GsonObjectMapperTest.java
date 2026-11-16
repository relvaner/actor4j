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
package io.actor4j.dataformat.gson;

import static org.junit.Assert.*;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import io.actor4j.core.json.ObjectMapper;
import io.actor4j.core.utils.GenericType;

// regression tests for the review 2026-10 (M1)
public class GsonObjectMapperTest {
	public record Plan(String name, int count, List<String> steps) {
	}

	public record Times(LocalDate date, LocalTime time, LocalDateTime dateTime, OffsetDateTime offset,
		ZonedDateTime zoned, Instant instant, Duration duration, Period period) {
	}

	public record Holder(Optional<String> value) {
	}

	protected final ObjectMapper objectMapper = ObjectMapper.create();

	@Test
	public void test_service_loader() {
		assertTrue(objectMapper instanceof ObjectMapperImpl);
	}

	@Test
	public void test_roundtrip_record() {
		Plan plan = new Plan("p", 2, List.of("a", "b"));

		assertEquals(plan, objectMapper.mapTo(objectMapper.mapFrom(plan), Plan.class));
	}

	@Test
	public void test_numbers_as_long_or_double() {
		Map<String, Object> map = objectMapper.mapTo("{\"i\":42,\"d\":4.5}", new GenericType<Map<String, Object>>() {});

		assertEquals(42L, map.get("i"));
		assertEquals(4.5, map.get("d"));
	}

	// M1: java.time

	@Test
	public void test_java_time_roundtrip() {
		Times times = new Times(
			LocalDate.of(2026, 11, 16), LocalTime.of(10, 15), LocalDateTime.of(2026, 11, 16, 10, 15, 30),
			OffsetDateTime.of(2026, 11, 16, 10, 15, 30, 0, ZoneOffset.ofHours(1)),
			ZonedDateTime.of(2026, 11, 16, 10, 15, 30, 0, ZoneId.of("Europe/Berlin")),
			Instant.parse("2026-11-16T09:15:30Z"), Duration.ofMinutes(90), Period.ofDays(3));

		assertEquals(times, objectMapper.mapTo(objectMapper.mapFrom(times), Times.class));
	}

	@Test
	public void test_java_time_as_iso_strings() {
		String json = objectMapper.mapFrom(Map.of("t", LocalDateTime.of(2026, 11, 16, 10, 15, 30)));

		assertEquals("{\"t\":\"2026-11-16T10:15:30\"}", json);
		assertEquals("\"10:15:00\"", objectMapper.mapFrom(LocalTime.of(10, 15)));
		assertNull(objectMapper.mapTo("null", LocalDate.class));
	}

	@Test(expected=IllegalArgumentException.class)
	public void test_invalid_java_time() {
		objectMapper.mapTo("\"not a date\"", LocalDate.class);
	}

	// M1: errors

	@Test(expected=IllegalArgumentException.class)
	public void test_invalid_json() {
		objectMapper.mapTo("{ not json", Map.class);
	}

	@Test(expected=IllegalArgumentException.class)
	public void test_invalid_json_generic_type() {
		objectMapper.mapTo("[1,", new GenericType<List<Integer>>() {});
	}

	@Test(expected=IllegalArgumentException.class)
	public void test_empty_json() {
		objectMapper.mapTo("", Map.class);
	}

	@Test(expected=IllegalArgumentException.class)
	public void test_wrong_type() {
		objectMapper.mapTo("\"text\"", Plan.class);
	}

	@Test(expected=IllegalArgumentException.class)
	public void test_jdk_type_without_adapter() {
		objectMapper.mapFrom(new Holder(Optional.of("v"))); // reflection on java.util.Optional is blocked
	}
}
