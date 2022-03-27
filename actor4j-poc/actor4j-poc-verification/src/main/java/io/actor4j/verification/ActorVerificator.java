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
package io.actor4j.verification;

import java.util.function.Consumer;

import org.jgrapht.Graph;

import io.actor4j.core.ActorSystem;
import io.actor4j.core.config.ActorSystemConfig;
import io.actor4j.verification.config.ActorVerificationConfig;
import io.actor4j.verification.internal.VerificatorActorSystemImpl;

public interface ActorVerificator extends ActorSystem {
	public static ActorVerificator create() {
		return create(null);
	}
	
	public static ActorVerificator create(ActorVerificationConfig config) {
		return new VerificatorActorSystemImpl(config!=null ? config : ActorVerificationConfig.create());
	}
	
	@Deprecated
	@Override
	public default boolean setConfig(ActorSystemConfig config) {
		return false;
	}
	
	public boolean setConfig(ActorVerificationConfig config);
	
	public void verify(Consumer<ActorVerificationSM> consumer);
	public void verifyAll(Consumer<ActorVerificationSM> consumer, Consumer<Graph<String, ActorVerificationEdge>> consumerAll);
}
