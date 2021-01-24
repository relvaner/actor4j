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
package io.actor4j.web.utils.rest.databind;

import java.util.List;

public class RESTActorResponse {
	protected String status;
	protected int code;
	protected Object data;
	protected String message;
	
	public static final String SUCCESS = "success";
	public static final String NO_SUCCESS = "no_success";
	public static final String ERROR = "error";
	
	public RESTActorResponse() {
		this("", 0,  "", "");
	}
	
	public RESTActorResponse(String status, int code, Object data, String message) {
		super();
		this.status = status;
		this.code = code;
		this.data = data;
		this.message = message;
	}

	public String getStatus() {
		return status;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}
	
	public int getCode() {
		return code;
	}
	
	public void setCode(int code) {
		this.code = code;
	}
	
	public Object getData() {
		return data;
	}
	
	public void setData(Object data) {
		this.data = data;
	}
	
	public List<?> dataAsList() {
		return (List<?>)data;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return "RESTActorResponse [status=" + status + ", code=" + code + ", data=" + data + ", message=" + message
				+ "]";
	}
}
