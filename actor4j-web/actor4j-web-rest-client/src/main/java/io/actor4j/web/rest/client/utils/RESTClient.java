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
package io.actor4j.web.rest.client.utils;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

public class RESTClient {
	public static Client createClient() {
		return ClientBuilder.newClient();
	}

	public static Response get(Client client, String uri, String resourceName, String acceptedResponseTypes,
			MultivaluedMap<String, Object> headers) {
		return client.target(uri).path(resourceName).request(acceptedResponseTypes).headers(headers).get();
	}

	public static Response post(Client client, String uri, String resourceName, String acceptedResponseTypes,
			MultivaluedMap<String, Object> headers, Entity<?> entity) {
		return client.target(uri).path(resourceName).request(acceptedResponseTypes).headers(headers).post(entity);
	}
}
