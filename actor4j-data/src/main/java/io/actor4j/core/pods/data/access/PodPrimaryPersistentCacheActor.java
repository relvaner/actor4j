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
package io.actor4j.core.pods.data.access;

import java.util.UUID;
import java.util.function.Function;

import io.actor4j.core.actors.ActorGroupMember;
import io.actor4j.core.actors.ActorIgnoreDistributedGroupMember;
import io.actor4j.core.data.access.AckMode;
import io.actor4j.core.data.access.PrimaryPersistentCacheActor;
import io.actor4j.core.utils.ActorFactory;
import io.actor4j.core.utils.ActorGroup;
import io.actor4j.core.pods.PodContext;

public abstract class PodPrimaryPersistentCacheActor<K, V> extends PrimaryPersistentCacheActor<K, V> implements ActorIgnoreDistributedGroupMember, ActorGroupMember {
	protected final PodContext context;
	
	public PodPrimaryPersistentCacheActor(ActorGroup group, String alias, Function<UUID, ActorFactory> secondary,
			int instances, int cacheSize, UUID dataAccess, AckMode ackMode, PodContext podContext) {
		super(group, alias, secondary, instances, cacheSize, dataAccess, ackMode);
		this.context = podContext;
	}

	public PodPrimaryPersistentCacheActor(String name, ActorGroup group, String alias,
			Function<UUID, ActorFactory> secondary, int instances, int cacheSize, UUID dataAccess, AckMode ackMode, PodContext podContext) {
		super(name, group, alias, secondary, instances, cacheSize, dataAccess, ackMode);
		this.context = podContext;
	}
}
