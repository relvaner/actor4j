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
package io.actor4j.web.websocket.endpoints;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import io.actor4j.web.websocket.WebSocketActorClientManager;

@ClientEndpoint
public class ActorClientEndpoint {
	@OnOpen
	public void onOpen(Session session) throws IOException {
	}
	 
	@OnMessage
	public String onMessage(String message, Session session) {
		String id   = message.substring(1, 1+36);
    	String data = message.substring(1+36);
		CompletableFuture<String> future = WebSocketActorClientManager.requestMap.get(id);
		future.complete(data);
		WebSocketActorClientManager.requestMap.remove(id);
		return null;
	}
	
	@OnClose
	public void onClose(Session session, CloseReason closeReason) throws InterruptedException {
	}
	
	@OnError
    public void onError(Session session, Throwable t) {
        t.printStackTrace();
    }
}
