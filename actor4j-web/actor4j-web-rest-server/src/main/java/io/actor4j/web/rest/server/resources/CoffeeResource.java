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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.actor4j.web.utils.rest.databind.RESTActorResponse;

@Path("/coffee")
public class CoffeeResource {
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getExample() {
		return Response.status(418).entity(
				new RESTActorResponse(RESTActorResponse.NO_SUCCESS, 418, "", "I'm a teapot.")).build();
	}
}
