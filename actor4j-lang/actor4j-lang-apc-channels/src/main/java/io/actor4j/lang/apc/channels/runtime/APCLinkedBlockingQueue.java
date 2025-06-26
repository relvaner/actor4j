/*
 * Copyright (c) 2015-2024, David A. Bauer. All rights reserved.
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
package io.actor4j.lang.apc.channels.runtime;

import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;

import io.actor4j.lang.apc.channels.Channel;

public class APCLinkedBlockingQueue<E> extends LinkedBlockingQueue<E> implements Channel<E> {
	private static final long serialVersionUID = -1872877762973881728L;

	public APCLinkedBlockingQueue() {
		super();
	}

	public APCLinkedBlockingQueue(Collection<? extends E> c) {
		super(c);
	}

	public APCLinkedBlockingQueue(int capacity) {
		super(capacity);
	}
}
