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
package io.actor4j.functions.pods.runtime;

import io.actor4j.functions.io.AsyncIORequest;
import io.actor4j.functions.pods.FunctionPodContext;
import io.actor4j.functions.query.AsyncQueryRequest;
import io.actor4j.functions.state.StateStore;
import io.actor4j.functions.streams.AsyncStreams;

public interface FunctionPodContextInternal extends FunctionPodContext {
	public void injectStateStore(StateStore<?, ?> stateStore);
	public void injectStreams(AsyncStreams streams);
	public void injectQueryRequest(AsyncQueryRequest queryRequest);
	public void injectIORequest(AsyncIORequest ioRequest);
}
