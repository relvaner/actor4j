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
package io.actor4j.web.utils;

import java.util.UUID;

public class TransferActorMessage {
	public Object value;
	public int tag;
	public UUID source;
	public UUID dest;
	
	public String destPath;
	
	public UUID id;
	
	public TransferActorMessage() {
		super();
		
		id = UUID.randomUUID();
	}
	
	public TransferActorMessage(Object value, int tag, UUID source, UUID dest) {
		this();
		this.value = value;
		this.tag = tag;
		this.source = source;
		this.dest = dest;
	}

	public TransferActorMessage(Object value, int tag, UUID source, String destPath) {
		this();
		this.value = value;
		this.tag = tag;
		this.source = source;
		this.destPath = destPath;
	}

	@Override
	public String toString() {
		return "TransferActorMessage [value=" + value + ", tag=" + tag + ", source=" + source + ", dest=" + dest
				+ ", destPath=" + destPath + ", id=" + id + "]";
	}
}