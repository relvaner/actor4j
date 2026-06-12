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
package io.actor4j.polyglot.pod;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;

import io.actor4j.core.actors.ActorRef;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.pods.PodContext;
import io.actor4j.polyglot.api.ActorPolyglotAPI;
import io.actor4j.polyglot.api.ActorPolyglotMessage;

public class PolyglotContext {
	protected final Context context;
	protected final String languageId;

	public static final String LANGUAGE_ID_JS      = "js";
	public static final String LANGUAGE_ID_PYTHON  = "python";
	
	public static final String API            = "api";
	public static final String MESSAGE        = "message";
	
	public static final String EXECUTE        = "execute";
	public static final String EXECUTE_PYTHON = "_internal_execute_wrapper";
	
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
	
	public static PolyglotContext create(String languageId) {
		return new PolyglotContext(languageId);
	}
	
	public PolyglotContext(String languageId, IOAccess ioAccess) {
		super();
		this.languageId = languageId;
		
		context = build(ioAccess);
	}
	
	public PolyglotContext(String languageId) {
		this(languageId, IOAccess.NONE);
	}
	
	public Context build(IOAccess ioAccess) {
		return Context.newBuilder(languageId)
			.engine(SHARED_ENGINE)
			.allowPolyglotAccess(PolyglotAccess.NONE)
			.allowHostAccess(HostAccess.EXPLICIT)
			.allowHostClassLookup(s -> false)
			.allowIO(ioAccess)
			.allowCreateThread(false)
			.allowNativeAccess(false)
			.allowAllAccess(false)
			.build();
	}

	public Value executeFunction(ActorRef host, PodContext podContext, ActorMessage<?> message, CharSequence script) {
		Value executeFunction = null;
		
		if (languageId.equalsIgnoreCase(LANGUAGE_ID_PYTHON)) {
			context.eval(languageId, script+WRAPPER_PYTHON);
			executeFunction = context.getBindings(languageId).getMember(PolyglotContext.EXECUTE_PYTHON);
		}
		else {
			context.eval(languageId, script);
			executeFunction = context.getBindings(languageId).getMember(PolyglotContext.EXECUTE);
		}
		
		if (executeFunction != null && executeFunction.canExecute())
			return executeFunction.execute(ActorPolyglotAPI.create(host, podContext), ActorPolyglotMessage.of(message));
		else
			return null;
	}
	
	public Context context() {
		return context;
	}

	public String languageId() {
		return languageId;
	}
	
	public void close() {
		context.close();
	}
}
