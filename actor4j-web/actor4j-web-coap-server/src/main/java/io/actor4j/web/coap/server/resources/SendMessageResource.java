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

import java.io.IOException;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.server.resources.CoapExchange;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.actor4j.core.ActorService;
import io.actor4j.web.utils.RemoteActorMessage;
import io.actor4j.web.utils.RemoteActorMessageDTO;
import io.actor4j.web.utils.rest.databind.COAPActorResponse;

public class SendMessageResource extends COAPActorResource {
	public SendMessageResource(ActorService service) {
		super("sendMessage", service);
		
		getAttributes().setTitle("SendMessage Resource");
	}
	
    public void handlePOST(CoapExchange exchange) {
		RemoteActorMessageDTO message = null;
		String error = null;
		try {
			message = new ObjectMapper().readValue(exchange.getRequestText(), RemoteActorMessageDTO.class);
		} catch (JsonParseException e) {
			error =  e.getMessage();
		} catch (JsonMappingException e) {
			error =  e.getMessage();
		} catch (IOException e) {
			error =  e.getMessage();
		}
		
		if (message!=null && message.dest()!=null)
			service.sendAsServer(new RemoteActorMessage<Object>(message.value(), message.tag(), message.source(), message.dest()));
		
		try {
			if (error==null && message.dest()!=null)
				exchange.respond(
					CoAP.ResponseCode.CONTENT, 
					new ObjectMapper().writeValueAsString(new COAPActorResponse(COAPActorResponse.SUCCESS, 202, "", "The request was accepted and the message was send.")),
					MediaTypeRegistry.APPLICATION_JSON
				);
			else
				exchange.respond(
					CoAP.ResponseCode.CONTENT, 
					new ObjectMapper().writeValueAsString(new COAPActorResponse(COAPActorResponse.ERROR, 400, error, "The request was error prone.")),
					MediaTypeRegistry.APPLICATION_JSON
				);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}
}
