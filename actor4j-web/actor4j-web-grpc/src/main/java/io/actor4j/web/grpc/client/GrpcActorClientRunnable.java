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

import static io.actor4j.core.logging.ActorLogger.*;

import java.io.IOException;
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import io.actor4j.core.ActorClientRunnable;
import io.actor4j.core.ActorServiceNode;
import io.actor4j.core.messages.ActorMessage;
import io.actor4j.web.grpc.ActorGrpcServiceOuterClass.ActorGRPCResponse;
import io.actor4j.web.utils.RemoteActorMessageDTO;
import io.grpc.ManagedChannel;

public class GrpcActorClientRunnable implements ActorClientRunnable {
	protected final Map<ActorServiceNode, ManagedChannel> channels;
	protected final List<ActorServiceNode> serviceNodes;
	protected final LoadingCache<UUID, Integer> cache;
	protected final LoadingCache<String, List<UUID>> cacheAlias;
	
	protected final Object lock = new Object();
	
	public GrpcActorClientRunnable(final List<ActorServiceNode> serviceNodes, int concurrencyLevel, int cachesize) {
		this.serviceNodes = serviceNodes;
		
		channels = new ConcurrentHashMap<>();
		
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
					ManagedChannel channel = getChannel(serviceNode);
					ActorGRPCResponse response = GrpcActorClientManager.hasActor(channel, UUID.randomUUID().toString(), uuid).get(2000, TimeUnit.MILLISECONDS);
					if (response.getMessage().equals("1")) {
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
					ManagedChannel channel = getChannel(serviceNode);
					ActorGRPCResponse response = GrpcActorClientManager.getActorsFromAlias(channel, UUID.randomUUID().toString(), alias).get(2000, TimeUnit.MILLISECONDS);
					if (!response.getMessage().equals("[]")) {
						result = new ObjectMapper().readValue(response.getMessage(), new TypeReference<List<UUID>>(){});
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
		UUID dest = message.dest();
		
		if (alias!=null) {
			List<UUID> uuids = null;
			if (!(uuids=cacheAlias.getUnchecked(alias)).isEmpty()) {
				if (uuids.size()==1)
					dest = uuids.get(0);
				else
					dest = uuids.get(ThreadLocalRandom.current().nextInt(uuids.size()));
			}
			else {
				logger().log(DEBUG, "The actor for a given alias was not found.");
				return;	
			}
		}
		
		int index;
		if ((index=cache.getUnchecked(dest))!=-1) {
			try {
				ManagedChannel channel = getChannel(serviceNodes.get(index));
				RemoteActorMessageDTO msg = new RemoteActorMessageDTO(message.value(), message.tag(), message.source(), dest);
				ActorGRPCResponse response = GrpcActorClientManager.sendMessage(channel, msg).get(2000, TimeUnit.MILLISECONDS);
				if (!response.getMessage().equals("1"))
					logger().log(DEBUG, "Message was not acknowledged.");
				
			} catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
				e.printStackTrace();
			}
		}
		else
			logger().log(DEBUG, "The actor was not found.");
	}
	
	@Override
	public void runViaPath(ActorMessage<?> message, ActorServiceNode node, String path) {
		try {
			ManagedChannel channel = getChannel(node);
			UUID dest = null;
			
			ActorGRPCResponse response = GrpcActorClientManager.getActorFromPath(channel, UUID.randomUUID().toString(), path).get(2000, TimeUnit.MILLISECONDS);
			dest = (!response.getMessage().equals("null")) ? new ObjectMapper().readValue(response.getMessage(), UUID.class) : null;
				
			if (dest!=null) {
				RemoteActorMessageDTO msg = new RemoteActorMessageDTO(message.value(), message.tag(), message.source(), dest);
				response = GrpcActorClientManager.sendMessage(channel, msg).get(2000, TimeUnit.MILLISECONDS);
				if (!response.getMessage().equals("1"))
					logger().log(DEBUG, "Message was not acknowledged.");
			}
				
		} catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
			e.printStackTrace();
		}
	}
	
	// uses Double-Check-Idiom a la Bloch
	protected ManagedChannel getChannel(ActorServiceNode serviceNode) {
		ManagedChannel result = channels.get(serviceNode);
		if (result==null) {
			synchronized(lock) {
				result = channels.get(serviceNode);
				if (result==null) {
					result = GrpcActorClientManager.connectToServer(serviceNode.uri());
					channels.put(serviceNode, result);
				}
			}
		}
		
		return result;
	}
	
	public void closeAll() {
		Iterator<Entry<ActorServiceNode, ManagedChannel>> iterator = channels.entrySet().iterator();
		
		while (iterator.hasNext())
			iterator.next().getValue().shutdown();
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
