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
package io.actor4j.core.data.access;

import io.actor4j.core.actors.ResourceActor;
import static io.actor4j.core.messages.ActorReservedTag.*;

public abstract class DataAccessActor<K, V> extends ResourceActor {
	public static final int  DEFAULT_MAX_FAILURES  = 5;
	public static final long DEFAULT_RESET_TIMEOUT = 30_000;
	
	public static final int HAS_ONE     = RESERVED_DATA_ACCESS_HAS_ONE;
	public static final int INSERT_ONE  = RESERVED_DATA_ACCESS_INSERT_ONE;
	public static final int REPLACE_ONE = RESERVED_DATA_ACCESS_REPLACE_ONE;
	public static final int UPDATE_ONE  = RESERVED_DATA_ACCESS_UPDATE_ONE;
	public static final int DELETE_ONE  = RESERVED_DATA_ACCESS_DELETE_ONE;
	public static final int FIND_ONE    = RESERVED_DATA_ACCESS_FIND_ONE;
	public static final int FIND_ALL    = RESERVED_DATA_ACCESS_FIND_ALL;
	public static final int FIND_NONE   = RESERVED_DATA_ACCESS_FIND_NONE;
	public static final int QUERY_ONE   = RESERVED_DATA_ACCESS_QUERY_ONE; // ONLY DIRECT ACCCESS
	public static final int QUERY_ALL   = RESERVED_DATA_ACCESS_QUERY_ALL; // ONLY DIRECT ACCCESS
	public static final int FLUSH       = RESERVED_DATA_ACCESS_FLUSH;
	
	public static final int SUCCESS     = RESERVED_DATA_ACCESS_SUCCESS;
	public static final int FAILURE     = RESERVED_DATA_ACCESS_FAILURE;
	
	public DataAccessActor(String name) {
		super(name);
	}

	public DataAccessActor(boolean stateful) {
		super(stateful);
	}

	public DataAccessActor(String name, boolean stateful, boolean bulk) {
		super(name, stateful, bulk);
	}

	public DataAccessActor(String name, boolean stateful) {
		super(name, stateful);
	}
}
