/*
 * Copyright (c) 2015-2020, David A. Bauer. All rights reserved.
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
package io.actor4j.streams.core;

import static io.actor4j.streams.core.runtime.ActorStreamsTag.DATA;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import io.actor4j.core.ActorSystem;
import io.actor4j.core.actors.Actor;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.ActorGroup;
import io.actor4j.core.utils.ActorGroupSet;
import io.actor4j.streams.core.runtime.StreamDecompActor;

public class ActorStreamManager {
	protected final ActorSystem system;
	protected /*quasi final*/ Runnable onStartup;
	protected /*quasi final*/ Runnable onTermination;
	
	protected final Map<UUID, List<?>> data;
	protected final Map<UUID, List<?>> result;
	protected final Map<String, UUID> aliases;
	
	protected final int nTasks;
	protected final boolean debugDataEnabled;
	
	public ActorStreamManager(ActorSystem system) {
		this(system, false);
	}
	
	public ActorStreamManager(ActorSystem system, boolean debugDataEnabled) {
		super();
		
		this.system = system;
		this.nTasks = system.getConfig().parallelism()*system.getConfig().parallelismFactor();
		this.debugDataEnabled = debugDataEnabled;
		
		data = new ConcurrentHashMap<>();
		result = new ConcurrentHashMap<>();
		aliases = new ConcurrentHashMap<>();
	}
	
	public ActorStreamManager onStartup(Runnable onStartup) {
		this.onStartup = onStartup;
		
		return this;
	}
	
	public ActorStreamManager onTermination(Runnable onTermination) {
		this.onTermination = onTermination;
		
		return this;
	}
	
	public void start(ActorStream<?, ?> process) {
		data.clear();
		result.clear();
		aliases.clear();
		
		final CountDownLatch countDownLatch = new CountDownLatch(1);
		process.configureAsRoot(this, countDownLatch);
		
		ActorId root = system.addActor(() ->
			new StreamDecompActor<>("node-"+process.node.id.toString(), process.node, result, aliases, debugDataEnabled, data)
		);

		system.send(ActorMessage.create(null, DATA, root, root));
		if (onStartup!=null)
			onStartup.run();
		try {
			countDownLatch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		system.send(ActorMessage.create(null, Actor.POISONPILL, system.SYSTEM_ID(), root));
		if (onTermination!=null)
			onTermination.run();
	}
	
	public void start(List<ActorStream<?, ?>> processes) {
		data.clear();
		result.clear();
		aliases.clear();
		
		final CountDownLatch countDownLatch = new CountDownLatch(processes.size());
		ActorGroup group = new ActorGroupSet();
		for (ActorStream<?, ?> process : processes) {
			process.configureAsRoot(this, countDownLatch);
			
			group.add(system.addActor(() ->
				new StreamDecompActor<>("node-"+process.node.id.toString(), process.node, result, aliases, debugDataEnabled, data))
			);
		}
		
		system.broadcast(ActorMessage.create(null, DATA, system.SYSTEM_ID(), null), group);
		if (onStartup!=null)
			onStartup.run();
		try {
			countDownLatch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		system.broadcast(ActorMessage.create(null, Actor.POISONPILL, system.SYSTEM_ID(), null), group);
		if (onTermination!=null)
			onTermination.run();
	}
	
	public void start(ActorStream<?, ?>... processes) {
		start(Arrays.asList(processes));
	}
	
	public List<?> getData(UUID id) { 
		return data.get(id);
	}
	
	public List<?> getData(String alias) {
		List<?> result = null;
		
		UUID id = aliases.get(alias);
		if (id!=null)
			result = getData(id);
		
		return result;
	}
	
	public List<?> getResult(UUID id) {
		return result.get(id);
	}
	
	public List<?> getFirstResult() {
		if (result.values().iterator().hasNext())
			return result.values().iterator().next();
		else
			return null;
	}
	
	public List<?> getResult(String alias) {
		List<?> result = null;
		
		UUID id = aliases.get(alias);
		if (id!=null)
			result = getResult(id);
		
		return result;
	}
}
