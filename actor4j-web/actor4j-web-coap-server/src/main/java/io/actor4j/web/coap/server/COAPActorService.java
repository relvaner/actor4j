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
package io.actor4j.web.coap.server;

import static io.actor4j.core.logging.ActorLogger.*;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.config.NetworkConfig;

import io.actor4j.core.ActorService;
import io.actor4j.web.coap.server.resources.CoffeeResource;
import io.actor4j.web.coap.server.resources.GetActorFromPathResource;
import io.actor4j.web.coap.server.resources.GetActorsFromAliasResource;
import io.actor4j.web.coap.server.resources.HasActorResource;
import io.actor4j.web.coap.server.resources.PingResource;
import io.actor4j.web.coap.server.resources.SendMessageResource;
import io.actor4j.web.coap.server.resources.VersionResource;

public abstract class COAPActorService extends CoapServer {
	public static final int COAP_PORT = NetworkConfig.getStandard().getInt(NetworkConfig.Keys.COAP_PORT);
	
	protected final ActorService service;

	public abstract ActorService getService();
	
	public COAPActorService() {
		super();
		
		service = getService();

		add(new CoffeeResource());
		add(new PingResource());
		add(new VersionResource());
		add(new HasActorResource(service));
		add(new GetActorsFromAliasResource(service));
		add(new GetActorFromPathResource(service));
		add(new SendMessageResource(service));
	}
	
	@Override
	public void start() {
		super.start();
		systemLogger().log(INFO, String.format("%s - COAP-Service started...", service.getName()));
	}
	
	public void shutdown() {
		stop();
		systemLogger().log(INFO, String.format("%s - COAP-Service stopped...", service.getName()));
	}
}
