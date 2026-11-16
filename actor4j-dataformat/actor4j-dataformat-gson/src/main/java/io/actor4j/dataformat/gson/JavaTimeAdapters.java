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

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.Function;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

// java.time as ISO-8601 strings (gson cannot reflect on JDK types)
final class JavaTimeAdapters {
	static GsonBuilder register(GsonBuilder builder) {
		return builder
			.registerTypeAdapter(LocalDate.class, adapter(DateTimeFormatter.ISO_LOCAL_DATE::format, LocalDate::parse))
			.registerTypeAdapter(LocalTime.class, adapter(DateTimeFormatter.ISO_LOCAL_TIME::format, LocalTime::parse))
			.registerTypeAdapter(LocalDateTime.class, adapter(DateTimeFormatter.ISO_LOCAL_DATE_TIME::format, LocalDateTime::parse))
			.registerTypeAdapter(OffsetDateTime.class, adapter(DateTimeFormatter.ISO_OFFSET_DATE_TIME::format, OffsetDateTime::parse))
			.registerTypeAdapter(ZonedDateTime.class, adapter(DateTimeFormatter.ISO_ZONED_DATE_TIME::format, ZonedDateTime::parse))
			.registerTypeAdapter(Instant.class, adapter(DateTimeFormatter.ISO_INSTANT::format, Instant::parse))
			.registerTypeAdapter(Duration.class, adapter(Duration::toString, Duration::parse))
			.registerTypeAdapter(Period.class, adapter(Period::toString, Period::parse));
	}

	private static <T> TypeAdapter<T> adapter(Function<T, String> format, Function<String, T> parse) {
		return new TypeAdapter<T>() {
			@Override
			public void write(JsonWriter out, T value) throws IOException {
				out.value(format.apply(value));
			}

			@Override
			public T read(JsonReader in) throws IOException {
				String text = in.nextString();
				try {
					return parse.apply(text);
				} catch (DateTimeParseException e) {
					throw new JsonParseException("invalid date/time: "+text, e);
				}
			}
		}.nullSafe();
	}
}
