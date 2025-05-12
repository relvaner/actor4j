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
package io.actor4j.core.data.access;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

import io.actor4j.core.id.ActorId;
import io.actor4j.core.utils.Pair;

public class AckWatcher<K> {
	protected Map<K, Set<Pair<ActorId, UUID>>> watcher;
	
	public AckWatcher() {
		super();
		watcher = new HashMap<>();
	}
	
	public void watch(K key, ActorId source, UUID interaction) {
		Set<Pair<ActorId, UUID>> set = watcher.get(key);
		if (set==null) {
			set = new HashSet<>();
			watcher.put(key, set);
		}
		set.add(Pair.of(source, interaction));
	}
	
	public void unwatch(K key, ActorId source, UUID interaction) {
		Set<Pair<ActorId, UUID>> set = watcher.get(key);
		if (set!=null)
			set.remove(Pair.of(source, interaction));
	}
	
	public Set<Pair<ActorId, UUID>> watchers(K key) {
		return watcher.get(key);
	}
	
	public void trigger(K key, BiConsumer<ActorId, UUID> handler) {
		Set<Pair<ActorId, UUID>> set = watcher.get(key);
		if (set!=null) {
			for (Pair<ActorId, UUID> pair : set)
				handler.accept(pair.left()/*source*/, pair.right()/*interaction*/);
			set.clear();
		}
	}
}
