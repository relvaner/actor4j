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

public abstract class DataAccessActor<K, V> extends ResourceActor {
	public static final int HAS_ONE     = 315;
	public static final int INSERT_ONE  = 316;
	public static final int REPLACE_ONE = 317;
	public static final int UPDATE_ONE  = 318;
	public static final int DELETE_ONE  = 319;
	public static final int FIND_ONE    = 320;
	public static final int FLUSH       = 321;
	
	public static final int FAILURE     = 322;
	
	public DataAccessActor(String name) {
		super(name);
	}
}
