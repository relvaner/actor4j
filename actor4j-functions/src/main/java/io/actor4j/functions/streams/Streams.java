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
package io.actor4j.functions.streams;

public class Streams implements StreamsHandler {
	protected final StreamsHandler delegate;
	
	public Streams(StreamsHandler delegate) {
		this.delegate = delegate;
	}
	
	public static Streams create(StreamsHandler delegate) {
		return new Streams(delegate);
	}
	
	@Override
	public void publish(String topic, Object value) {
		delegate.publish(topic, value);
	}

	@Override
	public void subscribe(String topic) {
		delegate.subscribe(topic);
	}
	
	@Override
	public void unsubscribe(String topic) {
		delegate.unsubscribe(topic);
	}
}
