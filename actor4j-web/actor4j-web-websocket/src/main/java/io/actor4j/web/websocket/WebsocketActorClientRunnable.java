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

import static io.actor4j.core.logging.user.ActorLogger.logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.websocket.Session;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import io.actor4j.core.ActorClientRunnable;
import io.actor4j.core.ActorServiceNode;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.web.utils.TransferActorMessage;
import io.actor4j.web.websocket.endpoints.ActorClientEndpoint;

public class WebsocketActorClientRunnable implements ActorClientRunnable {
	protected final Object annotatedEndpointInstance;
	
	protected final Map<ActorServiceNode, Session> sessions;
	protected final List<ActorServiceNode> serviceNodes;
	protected final LoadingCache<UUID, Integer> cache;
	protected final LoadingCache<String, List<UUID>> cacheAlias;
	
	protected final Object lock = new Object();
	
	public WebsocketActorClientRunnable(final List<ActorServiceNode> serviceNodes, int concurrencyLevel, int cachesize) {
		this.serviceNodes = serviceNodes;
		
		annotatedEndpointInstance = new ActorClientEndpoint();
		sessions = new ConcurrentHashMap<>();
		
		cache =  CacheBuilder.newBuilder()
				.maximumSize(cachesize)
				.concurrencyLevel(concurrencyLevel)
				.build(new CacheLoader<UUID, Integer>() {
			@Override
			public Integer load(UUID dest) throws Exception {
				String uuid = dest.toString();
				
				int found = -1;
				int i = 0;
				for (ActorServiceNode serviceNode : serviceNodes) {
					Session session = getSession(serviceNode);
					String response = WebSocketActorClientManager.hasActor(session, UUID.randomUUID().toString(), uuid).get(2000, TimeUnit.MILLISECONDS);
					if (response.equals("1")) {
						found = i;
						break;
							
					}
					i++;
				}
				
				return found;
			}
		});
		
		cacheAlias =  CacheBuilder.newBuilder()
				.maximumSize(cachesize)
				.concurrencyLevel(concurrencyLevel)
				.build(new CacheLoader<String, List<UUID>>() {
			@Override
			public List<UUID> load(String alias) throws Exception {
				List<UUID> result = new LinkedList<>();
				int i = 0;
				for (ActorServiceNode serviceNode : serviceNodes) {
					Session session = getSession(serviceNode);
					String response = WebSocketActorClientManager.getActorsFromAlias(session, UUID.randomUUID().toString(), alias).get(2000, TimeUnit.MILLISECONDS);
					if (!response.equals("[]")) {
						result = new ObjectMapper().readValue(response, new TypeReference<List<UUID>>(){});
						final int found = i;
						for (UUID id : result)
							cache.get(id, new Callable<Integer>() {
								@Override
								public Integer call() throws Exception {
									return found;
								}
							});
						break;
					}	
					i++;
				}
				
				return result;
			}
		});
	}
	
	@Override
	public void runViaAlias(ActorMessage<?> message, String alias) {
		if (alias!=null) {
			List<UUID> uuids = null;
			if (!(uuids=cacheAlias.getUnchecked(alias)).isEmpty()) {
				if (uuids.size()==1)
					message.dest = uuids.get(0);
				else
					message.dest = uuids.get(ThreadLocalRandom.current().nextInt(uuids.size()));
			}
			else {
				logger().debug("The actor for a given alias was not found.");
				return;	
			}
		}
		
		int index;
		if ((index=cache.getUnchecked(message.dest))!=-1) {
			try {
				Session session = getSession(serviceNodes.get(index));
				TransferActorMessage msg = new TransferActorMessage(message.value, message.tag, message.source, message.dest);
				String response = WebSocketActorClientManager.sendMessage(session, msg).get(2000, TimeUnit.MILLISECONDS);
				if (!response.equals("1")) {
					WebSocketActorClientManager.requestMap.remove(msg.id.toString());
					logger().debug("Message was not acknowledged.");
				}
				
			} catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
				e.printStackTrace();
			}
		}
		else
			logger().debug("The actor was not found.");
	}
		
	@Override
	public void runViaPath(ActorMessage<?> message, ActorServiceNode node, String path) {
		try {
			Session session = getSession(node);
			UUID dest = null;
			
			String response = WebSocketActorClientManager.getActorFromPath(session, UUID.randomUUID().toString(), path).get(2000, TimeUnit.MILLISECONDS);
			dest = (!response.equals("null")) ? new ObjectMapper().readValue(response, UUID.class) : null;
				
			if (dest!=null) {
				TransferActorMessage msg = new TransferActorMessage(message.value, message.tag, message.source, dest);
				response = WebSocketActorClientManager.sendMessage(session, msg).get(2000, TimeUnit.MILLISECONDS);
				if (!response.equals("1")) {
					WebSocketActorClientManager.requestMap.remove(msg.id.toString());
					logger().debug("Message was not acknowledged.");
				}
			}
				
		} catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
			e.printStackTrace();
		}
	}
	
	// not working properly
	/*
	public void runViaAliasAndPath(ImmutableList<ActorMessage<?>> messages) {
		List<Websocket> viaAlias = new LinkedList<>();
		List<Websocket> viaPath  = new LinkedList<>();
		
		for (ActorMessage<?> msg : messages.get()) {
			if (msg!=null && msg.value!=null && msg.value instanceof Websocket) {
				Websocket websocket = (Websocket)(msg.value);
				if (websocket.path==null)
					viaAlias.add(websocket);
				else
					viaPath.add(websocket);
				}
		}
		
		runViaAlias(viaAlias);
		runViaPath(viaPath);
	}
	
	public void runViaAlias(List<Websocket> messages) {
		Map<Integer, List<Websocket>> map = new HashMap<>();
		for (Websocket websocket : messages) {
			if (websocket.alias!=null) {
				List<UUID> uuids = null;
				if (!(uuids=cacheAlias.getUnchecked(websocket.alias)).isEmpty()) {
					if (uuids.size()==1)
						websocket.message.dest = uuids.get(0);
					else
						websocket.message.dest = uuids.get(ThreadLocalRandom.current().nextInt(uuids.size()));
				}
				else {
					logger().debug("The actor for a given alias was not found.");
					break;	
				}
			}
			
			int index;
			if (websocket.message.dest!=null && (index=cache.getUnchecked(websocket.message.dest))!=-1) {
				List<Websocket> list =  map.get(index);
				if (list==null) {
					list = new LinkedList<>();
					list.add(websocket);
					map.put(index, list);
				}
				else
					list.add(websocket);
			}
			else
				logger().debug("The actor was not found.");
		}
		
		Iterator<Entry<Integer, List<Websocket>>> iterator = map.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<Integer, List<Websocket>> entry = iterator.next();
			try {
				Session session = getSession(serviceNodes.get(entry.getKey()));
				
				List<TransferActorMessage> list = new ArrayList<>(entry.getValue().size());
				for (Websocket websocket : entry.getValue())
					list.add(new TransferActorMessage(websocket.message.value, websocket.message.tag, websocket.message.source, websocket.message.dest));
				
				BulkTransferActorMessage msg = new BulkTransferActorMessage(list);
				String response = WebSocketActorClientManager.sendMessage(session, msg).get(30, TimeUnit.SECONDS);
				if (!response.equals("1")) {
					WebSocketActorClientManager.requestMap.remove(msg.id.toString());
					logger().debug("Message was not acknowledged.");
				}
			} catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void runViaPath(List<Websocket> messages) {
		Map<ActorServiceNode, List<Websocket>> map = new HashMap<>();
		for (Websocket websocket : messages) {
			List<Websocket> list =  map.get(websocket.node);
			if (list==null) {
				list = new LinkedList<>();
				list.add(websocket);
				map.put(websocket.node, list);
			}
			else
				list.add(websocket);
		}
		
		Iterator<Entry<ActorServiceNode, List<Websocket>>> iterator = map.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<ActorServiceNode, List<Websocket>> entry = iterator.next();
			runViaPath(entry.getValue(), entry.getKey());
		}
	}
	
	public void runViaPath(List<Websocket> messages, ActorServiceNode node) {
		try {
			Session session = getSession(node);
			
			List<TransferActorMessage> list = new ArrayList<>(messages.size());
			for (Websocket websocket : messages)
				list.add(new TransferActorMessage(websocket.message.value, websocket.message.tag, websocket.message.source, (String)websocket.path));
			
			BulkTransferActorMessage msg = new BulkTransferActorMessage(list);
			String response = WebSocketActorClientManager.sendMessage(session, msg).get(30, TimeUnit.SECONDS);
			if (!response.equals("1")) {
				WebSocketActorClientManager.requestMap.remove(msg.id.toString());
				logger().debug("Message was not acknowledged.");
			}	
		} catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
			e.printStackTrace();
		}
	}
	*/
	
	// uses Double-Check-Idiom a la Bloch
	protected Session getSession(ActorServiceNode serviceNode) {
		Session result = sessions.get(serviceNode);
		if (result==null) {
			synchronized(lock) {
				result = sessions.get(serviceNode);
				if (result==null || !result.isOpen()) {
					try {
						result = WebSocketActorClientManager.connectToServer(annotatedEndpointInstance, new URI(serviceNode.getUri()));
						sessions.put(serviceNode, result);
					} catch (URISyntaxException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		return result;
	}
	
	public void closeAll() {
		Iterator<Entry<ActorServiceNode, Session>> iterator = sessions.entrySet().iterator();
		
		while (iterator.hasNext())
			try {
				iterator.next().getValue().close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
}
