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

import org.graalvm.polyglot.HostAccess.Export;
import org.graalvm.polyglot.Value;

import io.actor4j.core.actors.ActorRef;
import io.actor4j.core.messages.PodActorMessage;
import io.actor4j.core.pods.PodContext;

import static io.actor4j.core.logging.ActorLogger.*;

import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Level;

public class ActorPolyglotAPI {
	protected final ActorRef host;

	protected final PodContext context;
	
	protected final Function<Value, Object> defaultMapper;

	public ActorPolyglotAPI(ActorRef host, PodContext context, Function<Value, Object> defaultMapper) {
		super();
		this.host = host;
		this.context = context;
		this.defaultMapper = defaultMapper;
	}
	
	public ActorPolyglotAPI(ActorRef host, PodContext context) {
		this(host, context, (v) -> ValueMapper.convertValue(v));
	}

	public static ActorPolyglotAPI create(ActorRef host, PodContext context, Function<Value, Object> defaultMapper) {
		return new ActorPolyglotAPI(host, context, defaultMapper);
	}
	
	public static ActorPolyglotAPI create(ActorRef host, PodContext context) {
		return new ActorPolyglotAPI(host, context);
	}
	
	@Export
	public void send(Value value, int tag, String interaction, String dest) {
		send(value, tag, interaction, dest, null);
	}

	@Export
	public boolean send(Value value, int tag, String interaction, String dest, String protocol) {
		boolean result = false;

		if (dest != null) {
			host.sendViaGlobalId(
				PodActorMessage.create(
					defaultMapper.apply(value), 
					tag, 
					host.getId(), 
					null,
					interaction != null ? UUID.fromString(interaction) : null, 
					protocol, 
					context.domain()),
				UUID.fromString(dest));
			result = true;
		}

		return result;
	}

	protected void log(Level level, Object msg) {
		if (msg != null)
			logger().log(level, msg.toString());
		else
			logger().log(level, "null");
	}

	@Export
	public void error(Object msg) {
		log(ERROR, msg);
	};

	@Export
	public void warn(Object msg) {
		log(WARN, msg);
	};

	@Export
	public void info(Object msg) {
		log(INFO, msg);
	};

	@Export
	public void debug(Object msg) {
		log(DEBUG, msg);
	};

	@Export
	public void trace(String msg) {
		log(TRACE, msg);
	};
}
