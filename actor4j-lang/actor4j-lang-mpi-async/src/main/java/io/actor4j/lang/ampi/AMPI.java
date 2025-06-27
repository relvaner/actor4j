/*
 * Copyright (c) 2015-2024, David A. Bauer. All rights reserved.
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
package io.actor4j.lang.ampi;

/*
 *  Asynchronous Message Passing Interface (AMPI)
 *  
 *  Influenced by Open MPI 
 *  @See also: https://www.open-mpi.org/doc/v3.1/
 */
public interface AMPI {
	public int rank();
	public int size();
	
	public <T> void send(T t, int dest, int tag);
	public boolean probe(int source, int tag);
	public <T> T receive(int source, int tag);
	
	public <T> void asend(T t, int dest, int tag);
	public void await(int source, int tag);
}
