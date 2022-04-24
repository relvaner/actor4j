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
package io.actor4j.analyzer.runtime;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

import io.actor4j.core.runtime.ActorSystemImpl;
import io.actor4j.core.runtime.ActorThread;
import io.actor4j.core.runtime.DefaultActorMessageDispatcher;
import io.actor4j.core.messages.ActorMessage;

public class AnalyzerActorMessageDispatcher extends DefaultActorMessageDispatcher {
	public AnalyzerActorMessageDispatcher(ActorSystemImpl system) {
		super(system);
	}

	@Override
	public void post(ActorMessage<?> message, UUID source, String alias) {
		
		UUID dest = message.dest();
		if (alias!=null) {
			List<UUID> destinations = system.getActorsFromAlias(alias);

			dest = null;
			if (!destinations.isEmpty()) {
				if (destinations.size()==1)
					dest = destinations.get(0);
				else
					dest = destinations.get(ThreadLocalRandom.current().nextInt(destinations.size()));
			}
			if (dest==null)
				dest = ALIAS_ID;
		}
		UUID redirect = system.getRedirector().get(dest);
		if (redirect!=null) 
			dest = redirect;
		analyze(alias==null && redirect==null ? message.copy() : message.copy(dest));
		
		super.post(message, source, alias);
	}
	
	@Override
	protected void postQueue(ActorMessage<?> message, BiConsumer<ActorThread, ActorMessage<?>> biconsumer) {
		analyze(message.copy());
		super.postQueue(message, biconsumer);
	}
	
	@Override
	public void postOuter(ActorMessage<?> message) {
		analyze(message.copy());
		super.postOuter(message);
	}
	
	@Override
	public void postServer(ActorMessage<?> message) {
		analyze(message.copy());
		super.postServer(message);
	}
	
	protected void analyze(ActorMessage<?> message) {
		if (message!=null && ((AnalyzerActorSystemImpl)system).getAnalyzeMode().get())
				((AnalyzerActorSystemImpl)system).getAnalyzerThread().getOuterQueue().offer(message);
	}
}
