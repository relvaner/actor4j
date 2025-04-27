/*
 * Copyright (c) 2015-2025, David A. Bauer. All rights reserved.
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
package io.actor4j.benchmark.utils;

public class TimeMeasurement {
	
	protected long time      = 0;	
	protected long startTime = 0;
	protected long stopTime  = 0;	
	
	public void start() { 
		startTime = System.currentTimeMillis();
	}
  
	public void stop() {
		stopTime = System.currentTimeMillis();
    
		time = stopTime-startTime;
	}
	
	public boolean isMeasuring() {
		return (startTime!=0);
	}
	
	public void reset() {
		time      = 0;
		startTime = 0;
		stopTime  = 0;
	}
	
	public long getTime() {
		return time;
	}
  
	public String getTimeStr() {
		return String.valueOf(time) + " ms";
	}
}
