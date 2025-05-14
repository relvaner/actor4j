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

import java.util.List;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.server.resources.CoapExchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.actor4j.core.ActorService;
import io.actor4j.core.id.ActorId;
import io.actor4j.web.utils.rest.databind.COAPActorResponse;

public class GetActorsFromAliasResource extends COAPActorResource {
	public GetActorsFromAliasResource(ActorService service) {
		super("getActorsFromAlias", service);
			
		getAttributes().setTitle("GetActorsFromAlias Resource");
	}
		
	public void handleGET(CoapExchange exchange) {
		String alias = exchange.getQueryParameter("alias");
		List<ActorId> list = service.getActorsFromAlias(alias);
		if (!list.isEmpty()) {
			try {
				exchange.respond(
					CoAP.ResponseCode.CONTENT, 
					new ObjectMapper().writeValueAsString(new COAPActorResponse(COAPActorResponse.SUCCESS, 200, list.stream().map(id -> id.globalId()).toList(), "")),
					MediaTypeRegistry.APPLICATION_JSON
				);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		} else
			try {
				exchange.respond(
					CoAP.ResponseCode.CONTENT, 
					new ObjectMapper().writeValueAsString(new COAPActorResponse(COAPActorResponse.NO_SUCCESS, 200, "", "The actor(s) for a given alias was/were not found.")),
					MediaTypeRegistry.APPLICATION_JSON
				);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
	}
}
