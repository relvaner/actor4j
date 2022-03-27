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
package io.actor4j.web.grpc.server;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.actor4j.core.ActorService;
import io.actor4j.web.grpc.ActorGrpcServiceOuterClass;
import io.actor4j.web.grpc.ActorGrpcServiceGrpc.ActorGRPCServiceImplBase;
import io.actor4j.web.grpc.ActorGrpcServiceOuterClass.ActorGRPCResponse;
import io.actor4j.web.grpc.ActorGrpcServiceOuterClass.ActorGRPCResponse.Builder;
import io.actor4j.web.utils.RemoteActorMessage;
import io.actor4j.web.utils.RemoteActorMessageDTO;
import io.grpc.stub.StreamObserver;

public class ActorGrpcServiceImpl extends ActorGRPCServiceImplBase {
	public static final int HAS_ACTOR    		  = 1;
	public static final int GET_ACTORS_FROM_ALIAS = 2;
	public static final int GET_ACTOR_FROM_PATH   = 3;
	public static final int SEND_MESSAGE 		  = 4;
	public static final int CLIENT 	  		  	  = 5;
	
	protected ActorService service;
	
	public ActorGrpcServiceImpl(ActorService service) {
		super();
		this.service = service;
	}
	
	@Override
	public void send(ActorGrpcServiceOuterClass.ActorGRPCRequest request, StreamObserver<ActorGrpcServiceOuterClass.ActorGRPCResponse> responseObserver) {
		Builder builder = ActorGRPCResponse.newBuilder();
		builder
			.setTag(CLIENT)
			.setId(request.getId());
    	
    	switch (request.getTag()) {
    		case HAS_ACTOR : {
    			builder.setMessage((service.hasActor(request.getMessage())) ? "1" : "0");
    		}; break;
    		case GET_ACTORS_FROM_ALIAS : {
    			List<UUID> list = service.getActorsFromAlias(request.getMessage());
    			try {
					builder.setMessage((!list.isEmpty()) ? new ObjectMapper().writeValueAsString(list) : "[]");
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}
    		}; break;
    		case GET_ACTOR_FROM_PATH : {
    			UUID uuid = service.getActorFromPath(request.getMessage());
    			builder.setMessage((uuid!=null) ? uuid.toString() : "null");
    		}; break;
    		case SEND_MESSAGE : {
    			RemoteActorMessageDTO buf = null;
    			try {
    				buf = new ObjectMapper().readValue(request.getMessage(), RemoteActorMessageDTO.class);
    			} catch (Exception e) {
    				builder.setMessage("0");
    			}
    			
    			if (buf!=null) {
    				service.sendAsServer(new RemoteActorMessage<Object>(buf.value(), buf.tag(), buf.source(), buf.dest()));
    			
    				builder.setMessage("1");
    			}
    		};
    	}
    	
		responseObserver.onNext(builder.build());
		responseObserver.onCompleted();
	}
}
