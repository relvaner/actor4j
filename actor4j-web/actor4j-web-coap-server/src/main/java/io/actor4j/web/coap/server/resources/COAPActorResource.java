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
package io.actor4j.web.coap.server.resources;

import org.eclipse.californium.core.CoapResource;

import io.actor4j.core.ActorService;

public class COAPActorResource extends CoapResource {
	protected ActorService service;

	public COAPActorResource(String name, boolean visible, ActorService service) {
		super(name, visible);
		this.service = service;
	}

	public COAPActorResource(String name, ActorService service) {
		super(name);
		this.service = service;
	}
}
