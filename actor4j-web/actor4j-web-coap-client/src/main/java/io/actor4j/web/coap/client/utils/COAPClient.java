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
package io.actor4j.web.coap.client.utils;

import java.io.IOException;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.elements.exception.ConnectorException;

public class COAPClient {
	public static CoapClient createClient() {
		return new CoapClient();
	}

	public static CoapResponse get(CoapClient client, String uri, String resourceName) {
		CoapResponse result = null;
		try {
			result = client.setURI(uri+"/"+resourceName).get();
		} catch (ConnectorException | IOException e) {
			e.printStackTrace();
		}
		
		return result;
	}

	public static CoapResponse post(CoapClient client, String uri, String resourceName, String payload, int format) {
		CoapResponse result = null;
		try {
			result = client.setURI(uri+"/"+resourceName).post(payload, format /*MediaTypeRegistry*/);
		} catch (ConnectorException | IOException e) {
			e.printStackTrace();
		}
		
		return result;
	}
}
