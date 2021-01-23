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
package jade.benchmark;

import jade.core.Runtime;
import jade.util.leap.Properties;
import jade.wrapper.AgentContainer;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class Jade {
	protected Profile profile;
	protected AgentContainer container;
	
	protected String platformID;
	
	public Jade(String platformID) { 
		this.platformID = platformID;
		
        Properties properties = new Properties();
        properties.put(Profile.LOCAL_HOST, "localhost");
        properties.put(Profile.LOCAL_PORT, 1099);
        properties.put(Profile.PLATFORM_ID, platformID);
        properties.put(Profile.MAIN, true);
        properties.put(Profile.GUI, "false");
        
        profile = new ProfileImpl(properties);
	}
	
	public String getPlatformID() {
		return platformID;
	}
	
	public AgentContainer getContainer() {
		return container;
	}

	public void start() {
        container = Runtime.instance().createMainContainer(profile);
	}
	
	public void shutdown() {
		Runtime.instance().shutDown();
	}
	
	public AgentController addAgent(String nickname, String className, Object[] args) {
		AgentController result = null;
		System.out.println(container);
		try {	
			result = container.createNewAgent(nickname, className, args);
	        result.start();                               
	    } catch(StaleProxyException e) {
	    	throw new RuntimeException(e);
	    }
		
		return result;
	}
}
