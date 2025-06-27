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
import java.util.function.Function;

import io.actor4j.core.ActorRuntime;
import io.actor4j.core.config.ActorSystemConfig;
import io.actor4j.lang.ampi.AMPI;
import io.actor4j.lang.ampi.AMPILauncher;

public class Application2 {
	public static void main(String... args) {
		ActorSystemConfig config = ActorSystemConfig.builder()
			.parallelism(4)
			.build();
		AMPILauncher launcher = AMPILauncher.create(ActorRuntime.create(config));
		
		Function<AMPI, Boolean> task = (ampi) -> {
			boolean result = true;
			
			if (ampi.rank()==2) {
				int dest = 3;
				int msg = 27017;
				ampi.asend(msg, dest, 0);
				System.out.printf("rank %d has sent a message (%d) to rank %d%n", ampi.rank(), msg, dest);
			}
			else if (ampi.rank()==3) {
				int source = 2;
				int msg = 0;
				if (ampi.probe(source, 0)) {
					msg = ampi.receive(source, 0);
					System.out.printf("rank %d has received a message (%d) from rank %d%n", ampi.rank(), msg, source);
				}
				else {
					ampi.await(source, 0);
					result = false;
				}
			}
			else
				System.out.printf("Hello World from rank %d out of %d processes%n", ampi.rank(), ampi.size());

			return result;
		};
		
		launcher.run(task, config.parallelism());
	}
}
