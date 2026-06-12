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
package io.actor4j.polyglot.api;

import java.util.UUID;

import org.graalvm.polyglot.HostAccess.Export;

import io.actor4j.core.messages.ActorMessage;

public class ActorPolyglotMessage {
	protected ActorMessage<?> message;
	
	public ActorPolyglotMessage(ActorMessage<?> message) {
		this.message = message;
	}

	public static ActorPolyglotMessage of(ActorMessage<?> message) {
		return new ActorPolyglotMessage(message);
	}
	
	protected String idToString(UUID id) {
		if (id!=null)
			return id.toString();
		else
			return "null";
	}
	
	@Export
	public Object value() {
		return message.value();
	}
	
	@Export
	public int tag() {
		return message.tag();
	}
	
	@Export
	public String source() {
		return idToString(message.source().globalId());
	}
	
	@Export
	public String dest() {
		return idToString(message.dest().globalId());
	}
	
	@Export
	public String interaction() {
		return idToString(message.interaction());
	}
	
	@Export
	public String protocol() {
		return message.protocol();
	}
	
	@Export
	public String domain() {
		return message.domain();
	}
}
