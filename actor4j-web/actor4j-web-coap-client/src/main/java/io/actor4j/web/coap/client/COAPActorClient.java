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
package io.actor4j.web.coap.client;

import java.io.IOException;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.actor4j.web.coap.client.utils.COAPClient;
import io.actor4j.web.utils.TransferActorMessage;
import io.actor4j.web.utils.rest.databind.COAPActorResponse;

public class COAPActorClient extends COAPClient {
	public static CoapResponse version(CoapClient client, String uri) {
		return get(client, uri, "version");
	}
	
	public static CoapResponse hasActor(CoapClient client, String uri, String uuid) {
		return get(client, uri, "hasActor?uuid="+uuid);
	}
	
	public static CoapResponse getActorsFromAlias(CoapClient client, String uri, String alias) {
		return get(client, uri, "getActorsFromAlias?alias="+alias);
	}
	
	public static CoapResponse getActorFromPath(CoapClient client, String uri, String path) {
		return get(client, uri, "getActorFromPath?path="+path);
	}
	
	public static CoapResponse sendMessage(CoapClient client, String uri, String request) {
		return post(client, uri, "sendMessage", request, MediaTypeRegistry.APPLICATION_JSON);
	}
	
	public static CoapResponse sendMessage(CoapClient client, String uri, TransferActorMessage request) {
		CoapResponse result = null;
		
		try {
			result = sendMessage(client, uri, new ObjectMapper().writeValueAsString(request));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public static COAPActorResponse convert(CoapResponse response) {
		COAPActorResponse result = null;
		
		if (response.isSuccess())
			try {
				result = new ObjectMapper().readValue(response.getPayload(), COAPActorResponse.class);
			} catch (IOException e) {
				e.printStackTrace();
			}
		
		return result;
	}
}
