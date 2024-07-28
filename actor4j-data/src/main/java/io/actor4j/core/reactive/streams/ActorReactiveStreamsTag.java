/*
 * Copyright (c) 2015-2017, David A. Bauer. All rights reserved.
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
package io.actor4j.core.reactive.streams;

import static io.actor4j.core.messages.ActorReservedTag.*;

public final class ActorReactiveStreamsTag {
	public static final int SUBSCRIPTION_REQUEST       = RESERVED_REACTIVE_STREAMS_SUBSCRIPTION_REQUEST;
	public static final int SUBSCRIPTION_REQUEST_RESET = RESERVED_REACTIVE_STREAMS_SUBSCRIPTION_REQUEST_RESET;
	public static final int SUBSCRIPTION_CANCEL        = RESERVED_REACTIVE_STREAMS_SUBSCRIPTION_CANCEL;
	public static final int SUBSCRIPTION_BULK          = RESERVED_REACTIVE_STREAMS_SUBSCRIPTION_BULK;
	public static final int SUBSCRIPTION_CANCEL_BULK   = RESERVED_REACTIVE_STREAMS_SUBSCRIPTION_CANCEL_BULK;
	public static final int ON_NEXT                    = RESERVED_REACTIVE_STREAMS_ON_NEXT;
	public static final int ON_ERROR                   = RESERVED_REACTIVE_STREAMS_ON_ERROR;
	public static final int ON_COMPLETE                = RESERVED_REACTIVE_STREAMS_ON_COMPLETE;
}
