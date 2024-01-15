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
package io.actor4j.json.jackson.features;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;

import io.actor4j.core.json.ObjectMapper;
import io.actor4j.core.utils.GenericType;
import io.actor4j.core.utils.Pair;

public class ObjectMapperFeature {
	@Test
	public void test_01() {
		ObjectMapper objectMapper = ObjectMapper.create();
		
		List<Integer> sourcelist = List.of(567, 345, 859, 34);
		String json = objectMapper.mapFrom(sourcelist);
		List<Integer> targetlist = objectMapper.mapTo(json, new GenericType<List<Integer>>(){});
		
		assertEquals(sourcelist, targetlist);
	}
	
	@Test
	public void test_02() {
		ObjectMapper objectMapper = ObjectMapper.create();
		
		Pair<Integer, String> sourcePair = Pair.of(456, "hello");
		String json = objectMapper.mapFrom(sourcePair);
		Pair<Integer, String> targetPair = objectMapper.mapTo(json, new GenericType<Pair<Integer, String>>(){});
		
		assertEquals(sourcePair, targetPair);
	}
	
	@Test
	public void test_03() {
		com.fasterxml.jackson.databind.ObjectMapper objectMapper= new com.fasterxml.jackson.databind.ObjectMapper();
		objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		
		try {
			Pair<Integer, String> sourcePair = Pair.of(456, "hello");
			String json = objectMapper.writeValueAsString(sourcePair);
			Pair<Integer, String> targetPair = objectMapper.readValue(json, new TypeReference<Pair<Integer, String>>(){});
			
			assertEquals(sourcePair, targetPair);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}
}
