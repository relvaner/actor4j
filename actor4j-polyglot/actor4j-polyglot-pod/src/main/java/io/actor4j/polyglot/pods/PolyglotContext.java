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
package io.actor4j.polyglot.pods;

import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;
import org.graalvm.polyglot.Context.Builder;

import io.actor4j.core.actors.ActorRef;
import io.actor4j.core.id.ActorId;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.pods.PodContext;
import io.actor4j.polyglot.api.ActorPolyglotAPI;
import io.actor4j.polyglot.api.ActorPolyglotMessage;

public class PolyglotContext {
	protected final Context context;
	protected final String languageId;

	protected /*quasi final*/ ActorPolyglotAPI api;
	protected final Map<Long, ActorId> routes;

	public static final String LANGUAGE_ID_JS      = "js";
	public static final String LANGUAGE_ID_PYTHON  = "python";
	
	public static final String LANGUAGE_ID_JAVA    = "java";
	public static final String LANGUAGE_ID_WASM    = "wasm";
	
	public static final String API            = "api";
	public static final String MESSAGE        = "message";
	
	public static final String EXECUTE        = "execute";
	public static final String EXECUTE_PYTHON = "_internal_execute_wrapper";
	public static final String CLASS_JAVA     = "Script";
	
	public static final String VALUE          = "value";
	public static final String TAG            = "tag";

	public static final String ERROR          = "error";

	private static final String WRAPPER_PYTHON =
		"""


		from types import SimpleNamespace
		
		def _internal_execute_wrapper(api, message):
			raw_result = execute(api, message)
			
			if raw_result is None:
				return None

			if isinstance(raw_result, dict):
				return SimpleNamespace(**raw_result)
			return raw_result
		""";
	
	private static final Engine SHARED_ENGINE;

	static {
		SHARED_ENGINE = Engine.newBuilder().build();
	}
	
	public static PolyglotContext create(String languageId, Map<Long, ActorId> routes) {
		return new PolyglotContext(languageId, routes);
	}
	
	public PolyglotContext(String languageId, IOAccess ioAccess, Map<Long, ActorId> routes) {
		super();
		this.languageId = languageId;
		this.routes = routes;
		
		context = build(ioAccess);
	}
	
	public PolyglotContext(String languageId, Map<Long, ActorId> routes) {
		this(languageId, IOAccess.NONE, routes);
	}
	
	protected ActorPolyglotAPI createAPI(ActorRef host, PodContext podContext) {
		return ActorPolyglotAPI.create(host, podContext);
	}
	
	public Context build(IOAccess ioAccess) {
		Builder builder = Context.newBuilder(languageId)
			.engine(SHARED_ENGINE)
			.allowPolyglotAccess(PolyglotAccess.NONE)
			.allowHostAccess(HostAccess.EXPLICIT)
			.allowHostClassLookup((s) -> false)
			.allowIO(ioAccess)
			.allowCreateThread(false)
			.allowNativeAccess(false)
			.allowAllAccess(false);
		
		if (languageId.equalsIgnoreCase(LANGUAGE_ID_JAVA))
			builder.allowNativeAccess(true);
			
		return builder.build();
	}

	public Value executeFunction(ActorRef host, PodContext podContext, ActorMessage<?> message, CharSequence script) {
		Value executeFunction = null;
		
		if (languageId.equalsIgnoreCase(LANGUAGE_ID_JS)) {
			context.eval(languageId, script);
			executeFunction = context.getBindings(languageId).getMember(PolyglotContext.EXECUTE);
		}
		else if (languageId.equalsIgnoreCase(LANGUAGE_ID_PYTHON)) {
			context.eval(languageId, script+WRAPPER_PYTHON);
			executeFunction = context.getBindings(languageId).getMember(PolyglotContext.EXECUTE_PYTHON);
		}
		else if (languageId.equalsIgnoreCase(LANGUAGE_ID_JAVA)) {
			context.eval(languageId, script);
			executeFunction = context.getBindings(languageId).getMember(PolyglotContext.CLASS_JAVA).getMember(PolyglotContext.EXECUTE);
		}
		
		if (executeFunction != null && executeFunction.canExecute()) {
			if (api==null) {
				api = createAPI(host, podContext);
				api.router().putAll(routes);
			}
			
			return executeFunction.execute(api, ActorPolyglotMessage.of(message));
		}
		else
			return null;
	}
	
	public Context context() {
		return context;
	}

	public String languageId() {
		return languageId;
	}
	
	public ActorPolyglotAPI api() {
		return api;
	}
	
	public void close() {
		context.close();
	}
}
