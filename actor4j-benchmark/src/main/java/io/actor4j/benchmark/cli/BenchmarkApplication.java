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
package io.actor4j.benchmark.cli;

import java.lang.reflect.Constructor;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import io.actor4j.benchmark.BenchmarkConfig;

public class BenchmarkApplication {
	protected static final String VERSION = "2.1.0";
	
	public static String prefixToName(String prefix) {
		StringBuffer buffer = new StringBuffer("");
		
		String[] array = prefix.split("\\.");
		for (int i=0; i<array.length; i++) {
			String capitalized = array[i].substring(0, 1).toUpperCase();
			buffer.append(capitalized+array[i].substring(1));
		}
			
		return buffer.toString();
	}
	
	public void frameworkOptions(Options options) {
//		Option optionFrameworkActor4j = Option.builder("actor4j").desc("using the Actor4j framework").argName("actor4j").build();
//		options.addOption(optionFrameworkActor4j);
	}
	
	public String frameworkClasspath(CommandLine line) {
//		String result = null;
//		
//		if (line.hasOption("actor4j"))
//			result = "actor4j.benchmark.scenarios.";
//		
//		return result;
		return null;
	}
	
	public void main(String[] args) {
		Options options = new Options();
		
		frameworkOptions(options);
		
		Option optionClass = Option.builder("class").hasArg().desc("the class name prefix").argName("class").build();
		Option optionActors = Option.builder("actors").hasArg().desc("actors per thread").argName("actors").build();

		Option optionWarmupIterations = Option.builder("warmup").hasArg().desc("the warmup iterations for the benchmark").argName("warmup").build();
		Option optionDuration = Option.builder("duration").hasArg().desc("the benchmark duration").argName("duration").build();
		
		Option optionThreadMode = Option.builder("mode").hasArg().desc("the thread mode").argName("mode").build();
		Option optionParallelismMin = Option.builder("threads").hasArg().desc("the number of threads").argName("threads").build();
		Option optionParallelismFactor = Option.builder("factor").hasArg().desc("the parallelism factor").argName("factor").build();
		Option optionThroughput = Option.builder("throughput").hasArg().desc("the throughput").argName("throughput").build();
		
		Option optionParam1 = Option.builder("param1").desc("the first parameter").argName("param1").build();
		Option optionParam2 = Option.builder("param2").desc("the second parameter").argName("param2").build();
		
		options.addOption("?", "help", false, "print this message");
		options.addOption("version", false, "print the version information and exit");
				
		options.addOption(optionClass);
		options.addOption(optionActors);
		options.addOption(optionWarmupIterations);
		options.addOption(optionDuration);
		options.addOption(optionThreadMode);
		options.addOption(optionParallelismMin);
		options.addOption(optionParallelismFactor);
		options.addOption(optionThroughput);

		options.addOption(optionParam1);
		options.addOption(optionParam2);
		
		CommandLineParser parser = new DefaultParser();

		try {
			CommandLine line = parser.parse(options, args);

			if (line.hasOption("help")) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("benchmark [-options]",
						"Benchmark Application for Actor4j. Options:", options,
						"For more details, see https://github.com/relvaner/actor4j");
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
				config.duration = Long.valueOf(line.getOptionValue("duration"));
			if (line.hasOption("mode"))
				config.threadMode = line.getOptionValue("mode");
			if (line.hasOption("threads"))
				config.parallelism = Integer.valueOf(line.getOptionValue("threads"));
			if (line.hasOption("factor"))
				config.parallelismFactor = Integer.valueOf(line.getOptionValue("factor"));
			if (line.hasOption("throughput"))
				config.throughput = Integer.valueOf(line.getOptionValue("throughput"));
			if (line.hasOption("param1"))
				config.param1 = line.getOptionValue("param1");
			if (line.hasOption("param2"))
				config.param2 = line.getOptionValue("param2");
			
			if (line.hasOption("class")) {
				Class<?> clazz;
				try {
					String framework = frameworkClasspath(line);
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
