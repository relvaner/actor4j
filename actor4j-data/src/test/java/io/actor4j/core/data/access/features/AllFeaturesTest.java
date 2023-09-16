/*
 * Copyright (c) 2015-2017, David A. Bauer. All rights reserved.
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
package io.actor4j.core.data.access.features;

import org.junit.runners.Suite;

import io.actor4j.core.ActorRuntime;
import io.actor4j.core.ActorSystemFactory;

import static io.actor4j.core.logging.ActorLogger.ERROR;
import static io.actor4j.core.logging.ActorLogger.logger;
import static io.actor4j.core.logging.ActorLogger.systemLogger;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	VolatileCacheFeature.class,
	PersistentCacheFeature.class,
	IMSFeature.class,
	PubSubFeature.class
})
public class AllFeaturesTest {
	@BeforeClass
	public static void beforeClass() {
		systemLogger().setLevel(ERROR);
		logger().setLevel(ERROR);
	}
	
	public static ActorSystemFactory factory() {
		return ActorRuntime.factory();
	}
}
