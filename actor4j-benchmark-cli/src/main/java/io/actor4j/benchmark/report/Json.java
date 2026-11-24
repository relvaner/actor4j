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
package io.actor4j.benchmark.report;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

// JSON of the result files with the streaming API of jackson-core (JsonGenerator, JsonParser), no databind.
// Objects are Map<String, Object>, arrays List<Object>, field order is kept. NaN and infinity are written as
// strings like JMH does.
public final class Json {
	private static final JsonFactory FACTORY = new JsonFactory();

	private Json() {
	}

	public static String write(Object value) {
		StringWriter writer = new StringWriter();
		try (JsonGenerator generator = FACTORY.createGenerator(writer)) {
			generator.useDefaultPrettyPrinter();
			write(generator, value);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return writer.append('\n').toString();
	}

	protected static void write(JsonGenerator generator, Object value) throws IOException {
		if (value==null)
			generator.writeNull();
		else if (value instanceof String s)
			generator.writeString(s);
		else if (value instanceof Boolean b)
			generator.writeBoolean(b);
		else if (value instanceof Double || value instanceof Float) {
			double d = ((Number)value).doubleValue();
			if (Double.isNaN(d) || Double.isInfinite(d))
				generator.writeString(String.valueOf(d));
			else
				generator.writeNumber(d);
		}
		else if (value instanceof Long || value instanceof Integer || value instanceof Short || value instanceof Byte)
			generator.writeNumber(((Number)value).longValue());
		else if (value instanceof Number n)
			generator.writeNumber(n.doubleValue());
		else if (value instanceof Map<?, ?> map) {
			generator.writeStartObject();
			for (Map.Entry<?, ?> e : map.entrySet()) {
				generator.writeFieldName(String.valueOf(e.getKey()));
				write(generator, e.getValue());
			}
			generator.writeEndObject();
		}
		else if (value instanceof List<?> list) {
			generator.writeStartArray();
			for (Object element : list)
				write(generator, element);
			generator.writeEndArray();
		}
		else
			generator.writeString(String.valueOf(value));
	}

	public static Object parse(String text) {
		try (JsonParser parser = FACTORY.createParser(text)) {
			JsonToken token = parser.nextToken();
			if (token==null)
				throw new IllegalArgumentException("JSON: empty");
			Object result = read(parser, token);
			if (parser.nextToken()!=null)
				throw new IllegalArgumentException("JSON: unexpected content after the value");
			return result;
		} catch (IOException e) {
			throw new IllegalArgumentException("JSON: "+e.getMessage(), e);
		}
	}

	protected static Object read(JsonParser parser, JsonToken token) throws IOException {
		switch (token) {
			case START_OBJECT -> {
				Map<String, Object> result = new LinkedHashMap<>();
				while (parser.nextToken()==JsonToken.FIELD_NAME) {
					String key = parser.currentName();
					result.put(key, read(parser, parser.nextToken()));
				}
				return result;
			}
			case START_ARRAY -> {
				List<Object> result = new ArrayList<>();
				JsonToken next;
				while ((next = parser.nextToken())!=JsonToken.END_ARRAY)
					result.add(read(parser, next));
				return result;
			}
			case VALUE_STRING -> {
				return parser.getText();
			}
			case VALUE_NUMBER_INT -> {
				return parser.getLongValue();
			}
			case VALUE_NUMBER_FLOAT -> {
				return parser.getDoubleValue();
			}
			case VALUE_TRUE -> {
				return Boolean.TRUE;
			}
			case VALUE_FALSE -> {
				return Boolean.FALSE;
			}
			case VALUE_NULL -> {
				return null;
			}
			default -> throw new IllegalArgumentException("JSON: unexpected token "+token);
		}
	}

	// number from a parsed value (also "NaN" written as string)
	public static double number(Object value) {
		if (value instanceof Number n)
			return n.doubleValue();
		if (value instanceof String s)
			return Double.parseDouble(s);
		return Double.NaN;
	}
}
