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
package io.actor4j.polyglot.pods.runtime;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;
import org.graalvm.polyglot.Context.Builder;

import io.actor4j.core.actors.ActorRef;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.core.pods.PodContext;
import io.actor4j.polyglot.api.ActorPolyglotAPI;
import io.actor4j.polyglot.api.ActorPolyglotMessage;
import io.actor4j.polyglot.query.PolyglotQueryRequest;
import io.actor4j.polyglot.state.PolyglotStateStore;
import io.actor4j.polyglot.streams.PolyglotStreams;

public class PolyglotContextImpl implements PolyglotContextInternal {
	protected final Context context;
	protected final String languageId;

	protected /*quasi final*/ ActorPolyglotAPI api;
	protected /*quasi final*/ PolyglotStateStore stateStore;
	protected /*quasi final*/ PolyglotStreams streams;
	protected /*quasi final*/ PolyglotQueryRequest queryRequest;
	
//	public static final String API            = "api";
//	public static final String MESSAGE        = "message";
	
	public static final String EXECUTE        = "execute";
	public static final String EXECUTE_PYTHON = "_internal_execute_wrapper";
	public static final String CLASS_JAVA     = "Script";

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
	
	public static PolyglotContextImpl create(String languageId) {
		return new PolyglotContextImpl(languageId);
	}
	
	public PolyglotContextImpl(String languageId, IOAccess ioAccess) {
		super();
		this.languageId = languageId;
		
		context = build(ioAccess);
	}
	
	public PolyglotContextImpl(String languageId) {
		this(languageId, IOAccess.NONE);
	}
	
	@Override
	public void injectAPI(ActorPolyglotAPI api) {
		this.api = api;
	}
	
	@Override
	public void injectStateStore(PolyglotStateStore stateStore) {
		this.stateStore = stateStore;
	}
	
	@Override
	public void injectStreams(PolyglotStreams streams) {
		this.streams = streams;
	}
	
	@Override
	public void injectQueryRequest(PolyglotQueryRequest queryRequest) {
		this.queryRequest = queryRequest;
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

	@Override
	public Value executeFunction(ActorRef host, PodContext podContext, ActorMessage<?> message, CharSequence script) {
		Value executeFunction = null;
		
		if (languageId.equalsIgnoreCase(LANGUAGE_ID_JS)) {
			context.eval(languageId, script);
			executeFunction = context.getBindings(languageId).getMember(PolyglotContextImpl.EXECUTE);
		}
		else if (languageId.equalsIgnoreCase(LANGUAGE_ID_PYTHON)) {
			context.eval(languageId, script+WRAPPER_PYTHON);
			executeFunction = context.getBindings(languageId).getMember(PolyglotContextImpl.EXECUTE_PYTHON);
		}
		else if (languageId.equalsIgnoreCase(LANGUAGE_ID_JAVA)) {
			context.eval(languageId, script);
			executeFunction = context.getBindings(languageId).getMember(PolyglotContextImpl.CLASS_JAVA).getMember(PolyglotContextImpl.EXECUTE);
		}
		
		if (executeFunction != null && executeFunction.canExecute()) {
			if (streams==null) {
				if (stateStore!=null && queryRequest!=null)
					return executeFunction.execute(api, ActorPolyglotMessage.of(message), stateStore, queryRequest);
				else if (stateStore!=null)
					return executeFunction.execute(api, ActorPolyglotMessage.of(message), stateStore);
				else if (queryRequest!=null)
					return executeFunction.execute(api, ActorPolyglotMessage.of(message), queryRequest);
				else
					return executeFunction.execute(api, ActorPolyglotMessage.of(message));
			}
			else {
				if (stateStore!=null && queryRequest!=null)
					return executeFunction.execute(api, ActorPolyglotMessage.of(message), stateStore, queryRequest, streams);
				else if (stateStore!=null)
					return executeFunction.execute(api, ActorPolyglotMessage.of(message), stateStore, streams);
				else if (queryRequest!=null)
					return executeFunction.execute(api, ActorPolyglotMessage.of(message), queryRequest, streams);
				else
					return executeFunction.execute(api, ActorPolyglotMessage.of(message), streams);
			}
		}
		else
			return null;
	}
	
	@Override
	public Context context() {
		return context;
	}

	@Override
	public String languageId() {
		return languageId;
	}
	
	@Override
	public ActorPolyglotAPI api() {
		return api;
	}
	
	@Override
	public PolyglotStateStore stateStore() {
		return stateStore;
	}
	
	@Override
	public PolyglotStreams streams() {
		return streams;
	}
	
	@Override
	public PolyglotQueryRequest queryRequest() {
		return queryRequest;
	}
	
	@Override
	public void close() {
		context.close();
	}
}
