/*
 * Copyright (c) 2016-2019, David A. Bauer. All rights reserved.
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
package io.actor4j.apc.actor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.Future;

import io.actor4j.core.actors.Actor;
import io.actor4j.core.messages.ActorMessage;

// Asynchronous Procedure Call Actor
public class APCActor extends Actor {
	protected Class<?> interf;
	protected Object obj;
	
	public APCActor(Class<?> interf, Object obj) {
		super();
		this.interf = interf;
		this.obj = obj;
	}
	
	@Override
	public void preStart() {
		if (obj instanceof APCObject)
			((APCObject)obj).setSystem((APCActorSystem)getSystem());
	}

	@Override
	public void receive(ActorMessage<?> message) {
		if (message.value!=null && message.value instanceof APCActorPair) {
			APCActorPair pair = ((APCActorPair)message.value);
			
			Method method = null;
			if (pair.methodParams!=null) {
				try {
					Class<?>[] parameterTypes = new Class<?>[pair.methodParams.length];
					for(int i=0;  i<pair.methodParams.length; i++)
						parameterTypes[i] = pair.methodParams[i].getClass();
					
					method = interf.getMethod(((APCActorPair)message.value).methodName, parameterTypes);
				} catch (NoSuchMethodException | SecurityException e) {
					e.printStackTrace();
				}
				
				if (method!=null)
					try {
						handleOptionalFuture(method, message.interaction);
						method.invoke(obj, pair.methodParams);
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						e.printStackTrace();
					}
			}
			else {
				try {
					method = interf.getMethod(((APCActorPair)message.value).methodName);
				} catch (NoSuchMethodException | SecurityException e) {
					e.printStackTrace();
				}
				
				if (method!=null)
					try {
						handleOptionalFuture(method, message.interaction);
						method.invoke(obj);
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						e.printStackTrace();
					}
			}		
		}
		else 
			unhandled(message);
	}
	
	public void handleOptionalFuture(Method method, UUID interaction) {
		if (method.getReturnType()==Future.class)
			if (obj instanceof APCObject)
				((APCObject)obj).setCurrentFutureId(interaction);
	}
}
