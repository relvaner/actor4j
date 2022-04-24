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

import static io.actor4j.core.logging.ActorLogger.logger;

import java.io.IOException;

import io.actor4j.core.ActorService;
import io.actor4j.core.config.ActorServiceConfig;
import io.grpc.Server;
import io.grpc.ServerBuilder;

public abstract class ActorGrpcServer {
	protected ActorService service;
	protected Server server;
	
	protected abstract ActorService getService();

	public void start(String name, int port) {
		this.start(name, port, () -> service.start());
	}
	
	public void start(String name, int port, Runnable onStartup) {
		service = getService();
		deploy(service);
		ActorServiceConfig config = ActorServiceConfig.builder((ActorServiceConfig)service.getConfig())
			.name(name)
			.build();
		service.setConfig(config);
		
		try {
			server = ServerBuilder.forPort(port)
				.addService(new ActorGrpcServiceImpl(service)).build()
				.start();
			logger().info(String.format("%s - gRPC-Server started...", service.getConfig().name()));
			
			onStartup.run();
			logger().info(String.format("%s - Service started...", service.getConfig().name()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected abstract void deploy(ActorService service);
	
	public void awaitTermination() {		
		try {
			server.awaitTermination();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		service.shutdownWithActors(true);
		logger().info(String.format("%s - Service stopped...", service.getConfig().name()));
	}
}
