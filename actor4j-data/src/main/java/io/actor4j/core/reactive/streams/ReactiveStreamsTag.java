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

public final class ReactiveStreamsTag {
	public static final int SUBSCRIPTION_REQUEST       = 200;
	public static final int SUBSCRIPTION_REQUEST_RESET = 201;
	public static final int SUBSCRIPTION_CANCEL        = 202;
	public static final int SUBSCRIPTION_BULK          = 203;
	public static final int SUBSCRIPTION_CANCEL_BULK   = 204;
	public static final int ON_NEXT                    = 205;
	public static final int ON_ERROR                   = 206;
	public static final int ON_COMPLETE                = 207;
}
