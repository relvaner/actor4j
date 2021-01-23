/*
 * Copyright (c) 2015-2020, David A. Bauer. All rights reserved.
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
package shared.benchmark;

import java.lang.reflect.Constructor;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class BenchmarkSamplesApplication {
	protected static final String VERSION = "1.0.0";
	
	public static String prefixToName(String prefix) {
		StringBuffer buffer = new StringBuffer("");
		
		String[] array = prefix.split("\\.");
		for (int i=0; i<array.length; i++) {
			String capitalized = array[i].substring(0, 1).toUpperCase();
			buffer.append(capitalized+array[i].substring(1));
		}
			
		return buffer.toString();
	}
	
	public static void main(String[] args) {
		Options options = new Options();
		
		Option optionFrameworkOther = Option.builder("other").desc("using plain JAVA").argName("other").build();
		Option optionFrameworkAkka = Option.builder("akka").desc("using the Akka framework").argName("akka").build();
		Option optionFrameworkJADE = Option.builder("jade").desc("using the JADE framework").argName("jade").build();
		
		Option optionClass = Option.builder("class").hasArg().desc("the class name prefix").argName("class").build();
		Option optionActors = Option.builder("actors").hasArg().desc("actors per thread").argName("actors").build();

		Option optionWarmupIterations = Option.builder("warmup").hasArg().desc("the warmup iterations for the benchmark").argName("warmup").build();
		Option optionDuration = Option.builder("duration").hasArg().desc("the benchmark duration").argName("duration").build();
		Option optionTimes = Option.builder("times").hasArg().desc("the number of iterations").argName("times").build();
		
		Option optionParallelismMin = Option.builder("threads").hasArg().desc("the number of threads").argName("threads").build();
		Option optionParallelismFactor = Option.builder("factor").hasArg().desc("the parallelism factor").argName("factor").build();
		
		Option optionPingPongGroupedBenchmark = Option.builder("b_pingpong").desc("PingPong-Grouped benchmark (Actor4j)").argName("b_pingpong").build();
		Option optionNFoldRingBenchmark = Option.builder("b_ring").desc("NFoldRing benchmark (Actor4j)").argName("b_ring").build();
		
		options.addOption("?", "help", false, "print this message");
		options.addOption("version", false, "print the version information and exit");
		
		options.addOption(optionFrameworkOther);
		options.addOption(optionFrameworkAkka);
		options.addOption(optionFrameworkJADE);
		
		options.addOption(optionClass);
		options.addOption(optionActors);
		options.addOption(optionWarmupIterations);
		options.addOption(optionDuration);
		options.addOption(optionTimes);
		options.addOption(optionParallelismMin);
		options.addOption(optionParallelismFactor);
		
		options.addOption(optionPingPongGroupedBenchmark);
		options.addOption(optionNFoldRingBenchmark);
		
		CommandLineParser parser = new DefaultParser();

		try {
			CommandLine line = parser.parse(options, args);

			if (line.hasOption("help")) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("benchmark [-options]",
						"Benchmark Application for Actor4j. Options:", options,
						"For more details, see https://github.com/relvaner/actor4j-benchmark");
				return;
			} else if (line.hasOption("version")) {
				System.out.printf("benchmark version \"%s\"%n", VERSION);
				return;
			}
			
			BenchmarkConfig config = new BenchmarkConfig();
			
			if (line.hasOption("actors"))
				config.numberOfActors = Integer.valueOf(line.getOptionValue("actors"));
			if (line.hasOption("warmup"))
				config.warmupIterations = Integer.valueOf(line.getOptionValue("warmup"));
			if (line.hasOption("duration"))
				config.durationTimes = Long.valueOf(line.getOptionValue("duration"));
			if (line.hasOption("times"))
				config.durationTimes = Long.valueOf(line.getOptionValue("times"));
			if (line.hasOption("threads"))
				config.parallelismMin = Integer.valueOf(line.getOptionValue("threads"));
			if (line.hasOption("factor"))
				config.parallelismFactor = Integer.valueOf(line.getOptionValue("factor"));
			
			
			if (line.hasOption("class")) {
				Class<?> clazz;
				try {
					String framework = "actor4j.benchmark.samples.";
					if (line.hasOption("other"))
						framework = "other.benchmark.samples.";
					else if (line.hasOption("akka"))
						framework = "akka.benchmark.samples.";
					else if (line.hasOption("jade"))
						framework = "jade.benchmark.samples.";
					
					clazz = Class.forName(framework+line.getOptionValue("class")+".Benchmark"+prefixToName(line.getOptionValue("class")));
					Constructor<?> constructor = clazz.getConstructor(BenchmarkConfig.class);
					constructor.newInstance(config);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
		} catch (ParseException e) {
			System.err.println("Unexpected exception: " + e.getMessage());
		}
	}
}
