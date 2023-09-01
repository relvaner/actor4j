/*
 * Copyright (c) 2015-2023, David A. Bauer. All rights reserved.
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
package io.actor4j.cache.caffeine.features;
import static org.junit.Assert.assertEquals;

import javax.cache.Cache;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.MutableConfiguration;

import org.junit.Test;

import io.actor4j.cache.ActorCacheManager;

public class CaffeineCacheFeature {
	@Test
	public void test() {
		Configuration<String, String> configuration = new MutableConfiguration<>();
		Cache<String, String> cache = ActorCacheManager.createCache("test", configuration);
		
		cache.put("key01", "value01");
		cache.put("key02", "value02");
		cache.put("key03", "value03");
		
		assertEquals(cache.get("key01"), "value01");
		assertEquals(cache.get("key02"), "value02");
		assertEquals(cache.get("key03"), "value03");
	}
}
