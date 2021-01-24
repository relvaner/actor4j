/*
 * Copyright (c) 2015-2018, David A. Bauer. All rights reserved.
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
package io.actor4j.web.websocket.actors;

import io.actor4j.core.ActorServiceNode;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.utils.Shareable;

public class Websocket implements Shareable {
	public final ActorMessage<?> message;
	public final String alias;
	public final ActorServiceNode node;
	public final String path;
	
	public Websocket(ActorMessage<?> message, ActorServiceNode node, String path) {
		this.message = message;
		this.alias = null;
		this.node = node;
		this.path = path;
	}
	
	public Websocket(ActorMessage<?> message, String alias) {
		this.message = message;
		this.alias = alias;
		this.node = null;
		this.path = null;
	}
	
	public Websocket(ActorMessage<?> message) {
		this.message = message;
		this.alias = null;
		this.node = null;
		this.path = null;
	}

	@Override
	public String toString() {
		return "Websocket [message=" + message + ", alias=" + alias + ", node=" + node + ", path=" + path + "]";
	}
}
