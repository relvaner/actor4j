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
package io.actor4j.examples.coap;

import java.io.IOException;
import java.util.UUID;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.elements.exception.ConnectorException;

import io.actor4j.web.coap.client.COAPActorClient;
import io.actor4j.web.utils.TransferActorMessage;
import io.actor4j.web.utils.rest.databind.COAPActorResponse;

public class ExampleClientCoap {
	public static void main(String[] args) {
		CoapClient client = COAPActorClient.createClient();
		
		// Resource Discovery
		//System.out.println(COAPActorClient.get(client, "coap://localhost", ".well-known/core").getResponseText());
		try {
			System.out.println(client.setURI("coap://localhost").discover());
		} catch (ConnectorException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println(COAPActorClient.convert(COAPActorClient.version(client, "coap://localhost")));
		System.out.println(COAPActorClient.convert(COAPActorClient.hasActor(client, "coap://localhost", UUID.randomUUID().toString())));
		
		COAPActorResponse response =  COAPActorClient.convert(COAPActorClient.getActorsFromAlias(client, "coap://localhost", "coap"));
		System.out.println(response);
		UUID coap = UUID.fromString((String)response.dataAsList().get(0));
		System.out.println(COAPActorClient.convert(COAPActorClient.sendMessage(client, "coap://localhost", new TransferActorMessage("Hello World!", 0, UUID.randomUUID(), coap))));

		System.out.println(COAPActorClient.convert(COAPActorClient.getActorFromPath(client, "coap://localhost", "/")));
		System.out.println(COAPActorClient.convert(COAPActorClient.sendMessage(client, "coap://localhost", new TransferActorMessage("Hello World!", 0, null, UUID.randomUUID()))));
	}
}
