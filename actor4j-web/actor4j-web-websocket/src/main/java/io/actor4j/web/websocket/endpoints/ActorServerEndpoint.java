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

import static io.actor4j.core.logging.ActorLogger.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.actor4j.core.ActorService;
import io.actor4j.web.utils.BulkTransferActorMessage;
import io.actor4j.web.utils.RemoteActorMessage;
import io.actor4j.web.utils.TransferActorMessage;

@ServerEndpoint(value = "/actor4j")
public abstract class ActorServerEndpoint {
	public static final char HAS_ACTOR    		   = '1';
	public static final char GET_ACTORS_FROM_ALIAS = '2';
	public static final char GET_ACTOR_FROM_PATH   = '3';
	public static final char SEND_MESSAGE 		   = '4';
	public static final char SEND_BULK_MESSAGE     = '5';
	public static final char CLIENT 	  		   = '6';
	
	protected ActorService service;
	
	protected abstract ActorService getService();
	
	@OnOpen
	public void onOpen(Session session) throws IOException {
		service = getService();
		
		systemLogger().log(INFO, String.format("%s - Websocket-Session started...", service.getConfig().name()));
	}
	
    @OnMessage
    public void onMessage(String message, Session session) throws IOException {
    	String result = null;
    	
    	String id   = message.substring(1, 1+36);
    	String data = message.substring(1+36);
    	switch (message.charAt(0)) {
    		case HAS_ACTOR : {
    			result = (service.hasActor(data)) ? "1" : "0";
    			result = CLIENT + id + result;
    		}; break;
    		case GET_ACTORS_FROM_ALIAS : {
    			List<UUID> list = service.getActorsFromAlias(data);
    			result = (!list.isEmpty()) ? new ObjectMapper().writeValueAsString(list) : "[]";
    			result = CLIENT + id + result;
    		}; break;
    		case GET_ACTOR_FROM_PATH : {
    			UUID uuid = service.getActorFromPath(data);
    			result = (uuid!=null) ? uuid.toString() : "null";
    			result = CLIENT + id + result;
    		}; break;
    		case SEND_MESSAGE : {
    			TransferActorMessage buf = null;
    			try {
    				buf = new ObjectMapper().readValue(data, TransferActorMessage.class);
    			} catch (Exception e) {
    				result = CLIENT + id + "0";
    			}
    			
    			if (buf!=null) {
    				service.sendAsServer(new RemoteActorMessage<Object>(buf.value, buf.tag, buf.source, buf.dest));
    			
    				result = CLIENT + id + "1";
    			}
    		}; break;
    		case SEND_BULK_MESSAGE : {
    			BulkTransferActorMessage bulk_buf = null;
    			try {
    				bulk_buf = new ObjectMapper().readValue(data, BulkTransferActorMessage.class);
    			} catch (Exception e) {
    				result = CLIENT + id + "0";
    			}
    			
    			if (bulk_buf!=null) {
    				for (TransferActorMessage buf : bulk_buf.value) {
    					if (buf.dest==null && buf.destPath!=null) {
    						UUID dest = service.getActorFromPath(buf.destPath);
    		    			if (dest!=null)
    		    				service.sendAsServer(new RemoteActorMessage<Object>(buf.value, buf.tag, buf.source, dest));
    					}
    					else
    						service.sendAsServer(new RemoteActorMessage<Object>(buf.value, buf.tag, buf.source, buf.dest));
    				}
    			
    				result = CLIENT + id + "1";
    			}
    		};
    	}
    	
    	if (result!=null)
    		session.getBasicRemote().sendText(result);
    }
    
    @OnClose
	public void onClose(Session session, CloseReason closeReason) {
    	systemLogger().log(INFO, String.format("%s - Websocket-Session stopped...", service.getConfig().name()));
	}
    
    @OnError
    public void onError(Session session, Throwable t) {
        t.printStackTrace();
    }
}
