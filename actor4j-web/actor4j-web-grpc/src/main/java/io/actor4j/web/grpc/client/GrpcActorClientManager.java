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
package io.actor4j.web.grpc.client;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListenableFuture;

import io.actor4j.web.grpc.ActorGrpcServiceGrpc;
import io.actor4j.web.grpc.ActorGrpcServiceGrpc.ActorGRPCServiceFutureStub;
import io.actor4j.web.grpc.ActorGrpcServiceOuterClass.ActorGRPCRequest;
import io.actor4j.web.grpc.ActorGrpcServiceOuterClass.ActorGRPCResponse;
import io.actor4j.web.grpc.server.ActorGrpcServiceImpl;
import io.actor4j.web.utils.RemoteActorMessageDTO;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class GrpcActorClientManager {
	public static Map<ManagedChannel, Lock> lockMap;
	protected static final Object internal_lock = new Object();
	
	public static final int HAS_ACTOR             = ActorGrpcServiceImpl.HAS_ACTOR;
	public static final int GET_ACTORS_FROM_ALIAS = ActorGrpcServiceImpl.GET_ACTORS_FROM_ALIAS;
	public static final int GET_ACTOR_FROM_PATH   = ActorGrpcServiceImpl.GET_ACTOR_FROM_PATH;
	public static final int SEND_MESSAGE          = ActorGrpcServiceImpl.SEND_MESSAGE;
	
	static {
		lockMap = new ConcurrentHashMap<>();
	}
	
	private GrpcActorClientManager() {
	}
	
	public static ManagedChannel connectToServer(String target) {
		return ManagedChannelBuilder.forTarget(target)/*.keepAliveWithoutCalls(true)*/.usePlaintext().build();
	}
	
	public static ListenableFuture<ActorGRPCResponse> sendText(ManagedChannel channel, int tag, String id, String message) {
		ListenableFuture<ActorGRPCResponse> result = null;
		// uses Double-Check-Idiom a la Bloch
		Lock lock = lockMap.get(channel);
		if (lock==null) {
			synchronized(internal_lock) {
				lock = lockMap.get(channel);
				if (lock==null) {
					lock = new ReentrantLock();
					lockMap.put(channel, lock);
				}
			}
		}		
		lock.lock();
		try {
			ActorGRPCServiceFutureStub futureStub = ActorGrpcServiceGrpc.newFutureStub(channel);
			ActorGRPCRequest request = ActorGRPCRequest.newBuilder()
				.setTag(tag)
				.setId(id)
				.setMessage(message)
				.build();
			
			result = futureStub.send(request);
		}
		finally {
			lock.unlock();
		}
        return result;
	}
	
	public static ListenableFuture<ActorGRPCResponse> getActorsFromAlias(ManagedChannel channel, String id, String alias) throws IOException, InterruptedException, ExecutionException  {
		return sendText(channel, GET_ACTORS_FROM_ALIAS, id, alias);
	}
	
	public static ListenableFuture<ActorGRPCResponse> getActorFromPath(ManagedChannel channel, String id, String path) throws IOException, InterruptedException, ExecutionException  {
		return sendText(channel, GET_ACTOR_FROM_PATH, id, path);
	}
	
	public static ListenableFuture<ActorGRPCResponse> hasActor(ManagedChannel channel, String id, String uuid) throws IOException, InterruptedException, ExecutionException  {
		return sendText(channel, HAS_ACTOR, id, uuid);
	}
	
	public static ListenableFuture<ActorGRPCResponse> sendMessage(ManagedChannel channel, RemoteActorMessageDTO message) throws IOException, InterruptedException, ExecutionException  {
		return sendText(channel, SEND_MESSAGE, message.id().toString(), new ObjectMapper().writeValueAsString(message));
	}
	
	public static List<UUID> getActorsFromAliasSync(ManagedChannel channel, String id, String alias) throws IOException, InterruptedException, ExecutionException  {
		return new ObjectMapper().readValue(getActorsFromAlias(channel, id, alias).get().getMessage(), new TypeReference<List<UUID>>(){});
	}
	
	public static UUID getActorFromPathSync(ManagedChannel channel, String id, String path) throws IOException, InterruptedException, ExecutionException  {
		String result = getActorFromPath(channel, id, path).get().getMessage();
		
		return (!result.equals("null")) ? new ObjectMapper().readValue(result, UUID.class) : null;
	}
	
	public static Boolean hasActorSync(ManagedChannel channel, String id, String uuid) throws IOException, InterruptedException, ExecutionException  {
		return hasActor(channel, id, uuid).get().getMessage().equals("1")? true:false;
	}
	
	public static Boolean sendMessageSync(ManagedChannel channel, RemoteActorMessageDTO message) throws IOException, InterruptedException, ExecutionException  {
		return sendMessage(channel, message).get().getMessage().equals("1")? true:false;
	}
}
