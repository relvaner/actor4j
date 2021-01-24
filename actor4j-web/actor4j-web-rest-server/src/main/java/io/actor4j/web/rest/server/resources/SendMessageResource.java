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
package io.actor4j.web.rest.server.resources;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.actor4j.core.ActorService;
import io.actor4j.core.messages.RemoteActorMessage;
import io.actor4j.web.utils.TransferActorMessage;
import io.actor4j.web.utils.rest.databind.RESTActorResponse;

@Path("/sendMessage")
public class SendMessageResource {
	@Context 
	ActorService service;
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
    public Response sendMessage(String json) {
		TransferActorMessage message = null;
		String error = null;
		try {
			message = new ObjectMapper().readValue(json, TransferActorMessage.class);
		} catch (JsonParseException e) {
			error =  e.getMessage();
		} catch (JsonMappingException e) {
			error =  e.getMessage();
		} catch (IOException e) {
			error =  e.getMessage();
		}
		
		if (message!=null && message.dest!=null)
			service.sendAsServer(new RemoteActorMessage<Object>(message.value, message.tag, message.source, message.dest));
		
		if (error==null && message.dest!=null)
			return Response.status(202).entity(
					new RESTActorResponse(
							RESTActorResponse.SUCCESS, 202, "", "The request was accepted and the message was send.")).build();
		else
			return Response.status(400).entity(
					new RESTActorResponse(
							RESTActorResponse.ERROR, 400, error, "The request was error prone.")).build();
	}
}
