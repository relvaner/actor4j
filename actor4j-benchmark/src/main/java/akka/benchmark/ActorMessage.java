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
package akka.benchmark;

import java.util.List;

public class ActorMessage {
	public Object value;
	public int tag;
	
	public ActorMessage(Object value, int tag) {
		super();
		this.value = value;
		this.tag = tag;
	}
	
	public ActorMessage(Object value, Enum<?> tag) {
		this(value, tag.ordinal());
	}
	
	public ActorMessage(int tag) {
		this(null, tag);
	}
	
	public ActorMessage(Enum<?> tag) {
		this(tag.ordinal());
	}
	
	public boolean valueAsBooolean() {
		return (boolean)value;
	}
	
	public int valueAsInt() {
		return (int)value;
	}
	
	public long valueAsLong() {
		return (long)value;
	}
	
	
	public double valueAsDouble() {
		return (double)value;
	}
	
	public String valueAsString() {
		return (String)value;
	}
	
	public List<?> valueAsList() {
		return (List<?>)value;
	}
}
