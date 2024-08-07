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
package io.actor4j.streams.core.runtime;

import static io.actor4j.core.messages.ActorReservedTag.*;

public final class ActorStreamsTag {
	public static final int DATA     = RESERVED_STREAMS_DATA;
	public static final int TASK     = RESERVED_STREAMS_TASK;
	public static final int REDUCE   = RESERVED_STREAMS_REDUCE;
	public static final int RESULT   = RESERVED_STREAMS_RESULT;
	public static final int SHUTDOWN = RESERVED_STREAMS_SHUTDOWN;
}
