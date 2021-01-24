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
package io.actor4j.web.rest.client;

import java.io.IOException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.actor4j.web.rest.client.utils.RESTClient;
import io.actor4j.web.utils.TransferActorMessage;
import io.actor4j.web.utils.rest.databind.RESTActorResponse;

public class RESTActorClient extends RESTClient {
	public static Response version(Client client, String uri) {
		return get(client, uri, "version", "application/json; charset=UTF-8", null);
	}
	
	public static Response hasActor(Client client, String uri, String uuid) {
		return get(client, uri, "hasActor/"+uuid, "application/json; charset=UTF-8", null);
	}
	
	public static Response getActorFromAlias(Client client, String uri, String alias) {
		return get(client, uri, "getActorFromAlias/"+alias, "application/json; charset=UTF-8", null);
	}
	
	public static Response getActorFromPath(Client client, String uri, String path) {
		return get(client, uri, "getActorFromPath/"+path, "application/json; charset=UTF-8", null);
	}
	
	public static Response sendMessage(Client client, String uri, String request) {
		return post(client, uri, "sendMessage", "application/json; charset=UTF-8", null, Entity.entity(request, MediaType.APPLICATION_JSON));
	}
	
	public static Response sendMessage(Client client, String uri, TransferActorMessage request) {
		Response result = null;
		
		try {
			result = sendMessage(client, uri, new ObjectMapper().writeValueAsString(request));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public static RESTActorResponse convert(Response response) {
		RESTActorResponse result = null;
		
		if (response.hasEntity())
			try {
				result = new ObjectMapper().readValue(response.readEntity(String.class), RESTActorResponse.class);
			} catch (IOException e) {
				e.printStackTrace();
			}
		
		return result;
	}
}
