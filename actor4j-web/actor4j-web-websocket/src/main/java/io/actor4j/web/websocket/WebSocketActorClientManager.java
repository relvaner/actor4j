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
package io.actor4j.web.websocket;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.actor4j.web.utils.BulkTransferActorMessage;
import io.actor4j.web.utils.TransferActorMessage;
import io.actor4j.web.websocket.endpoints.ActorServerEndpoint;

// @See: https://blogs.oracle.com/pavelbucek/is-websocket-session-really-thread-safe
public class WebSocketActorClientManager {
	public static Map<String, CompletableFuture<String>> requestMap;
	public static Map<Session, Lock> lockMap;
	protected static final Object internal_lock = new Object();
	
	public static final String HAS_ACTOR             = String.valueOf(ActorServerEndpoint.HAS_ACTOR);
	public static final String GET_ACTORS_FROM_ALIAS = String.valueOf(ActorServerEndpoint.GET_ACTORS_FROM_ALIAS);
	public static final String GET_ACTOR_FROM_PATH   = String.valueOf(ActorServerEndpoint.GET_ACTOR_FROM_PATH);
	public static final String SEND_MESSAGE          = String.valueOf(ActorServerEndpoint.SEND_MESSAGE);
	public static final String SEND_BULK_MESSAGE     = String.valueOf(ActorServerEndpoint.SEND_BULK_MESSAGE);
	
	static {
		requestMap = new ConcurrentHashMap<>();
		lockMap = new ConcurrentHashMap<>();
	}
	
	private WebSocketActorClientManager() {
	}
	
	public static Session connectToServer(Class<?> annotatedEndpointClass, URI path) {
		Session result = null;
		WebSocketContainer container = ContainerProvider.getWebSocketContainer();
		try {
			result = container.connectToServer(annotatedEndpointClass, path);
		} catch (DeploymentException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public static Session connectToServer(Object annotatedEndpointInstance, URI path) {
		Session result = null;
		WebSocketContainer container = ContainerProvider.getWebSocketContainer();
		try {
			result = container.connectToServer(annotatedEndpointInstance, path);
		} catch (DeploymentException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public static CompletableFuture<String> sendText(Session session, String id, String message) throws IOException, InterruptedException, ExecutionException {
		CompletableFuture<String> result = new CompletableFuture<>();
		requestMap.put(id, result);
		// @See https://blogs.oracle.com/pavelbucek/is-websocket-session-really-thread-safe
		// uses Double-Check-Idiom a la Bloch
		Lock lock = lockMap.get(session);
		if (lock==null) {
			synchronized(internal_lock) {
				lock = lockMap.get(session);
				if (lock==null) {
					lock = new ReentrantLock();
					lockMap.put(session, lock);
				}
			}
		}
		lock.lock();
		try {
			session.getBasicRemote().sendText(message);
		}
		finally {
			lock.unlock();
		}
        return result;
	}
	
	public static CompletableFuture<String> sendText(Session session, String id, String tag, String message) throws IOException, InterruptedException, ExecutionException {
		return sendText(session, id, tag+id+message);
	}
	
	public static CompletableFuture<String> getActorsFromAlias(Session session, String id, String alias) throws IOException, InterruptedException, ExecutionException  {
		return sendText(session, id, GET_ACTORS_FROM_ALIAS, alias);
	}
	
	public static CompletableFuture<String> getActorFromPath(Session session, String id, String path) throws IOException, InterruptedException, ExecutionException  {
		return sendText(session, id, GET_ACTOR_FROM_PATH, path);
	}
	
	public static CompletableFuture<String> hasActor(Session session, String id, String uuid) throws IOException, InterruptedException, ExecutionException  {
		return sendText(session, id, HAS_ACTOR, uuid);
	}
	
	public static CompletableFuture<String> sendMessage(Session session, TransferActorMessage message) throws IOException, InterruptedException, ExecutionException  {
		return sendText(session, message.id.toString(), SEND_MESSAGE, new ObjectMapper().writeValueAsString(message));
	}
	
	public static CompletableFuture<String> sendMessage(Session session, BulkTransferActorMessage messages) throws IOException, InterruptedException, ExecutionException {
		return sendText(session, messages.id.toString(), SEND_BULK_MESSAGE, new ObjectMapper().writeValueAsString(messages));
	}
	
	public static List<UUID> getActorsFromAliasSync(Session session, String id, String alias) throws IOException, InterruptedException, ExecutionException  {
		return new ObjectMapper().readValue(getActorsFromAlias(session, id, alias).get(), new TypeReference<List<UUID>>(){});
	}
	
	public static UUID getActorFromPathSync(Session session, String id, String path) throws IOException, InterruptedException, ExecutionException  {
		String result = getActorFromPath(session, id, path).get();
		
		return (!result.equals("null")) ? new ObjectMapper().readValue(result, UUID.class) : null;
	}
	
	public static Boolean hasActorSync(Session session, String id, String uuid) throws IOException, InterruptedException, ExecutionException  {
		return hasActor(session, id, uuid).get().equals("1")? true:false;
	}
	
	public static Boolean sendMessageSync(Session session, TransferActorMessage message) throws IOException, InterruptedException, ExecutionException  {
		return sendMessage(session, message).get().equals("1")? true:false;
	}
}
