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

import java.util.UUID;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.actor4j.core.ActorService;
import io.actor4j.web.utils.rest.databind.RESTActorResponse;

@Path("/getActorFromPath/{path}")
public class GetActorFromPathResource {
	@Context
	ActorService service;

	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getActorFromPath(@PathParam("path") String path) {
		UUID uuid = service.getActorFromPath(path);
		if (uuid != null)
			return Response.ok().entity(
					new RESTActorResponse(
							RESTActorResponse.SUCCESS, 200, uuid.toString(), "")).build();
		else
			return Response.ok().entity(
					new RESTActorResponse(
							RESTActorResponse.NO_SUCCESS, 200, "", "The actor for a given path was not found.")).build();
	}
}
