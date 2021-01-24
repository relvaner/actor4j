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
package io.actor4j.web.rest.server;

import static io.actor4j.core.logging.user.ActorLogger.*;

import javax.annotation.PreDestroy;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import io.actor4j.core.ActorService;

public abstract class RESTActorService extends ResourceConfig {
	protected final ActorService service;

	public abstract ActorService getService();
	
	public RESTActorService() {
		super();
		
		service = getService();
		
		logger().info(String.format("%s - REST-Service started...", service.getName()));
		
		packages("io.actor4j.web.rest.server");

		register(new AbstractBinder() {
			protected void configure() {
				bind(service).to(ActorService.class);
			}
		});

		register(new JacksonJsonProvider().configure(SerializationFeature.INDENT_OUTPUT, true));
	}
	
	@PreDestroy
	public void shutdown() {
		logger().info(String.format("%s - REST-Service stopped...", service.getName()));
	}
}
