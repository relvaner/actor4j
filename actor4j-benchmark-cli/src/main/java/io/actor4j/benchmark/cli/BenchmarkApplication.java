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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.help.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import io.actor4j.benchmark.BenchmarkConfig;

public class BenchmarkApplication {
	// from the jar manifest (Implementation-Version), "unknown" when run from classes
	protected static final String VERSION = version();
	
	protected static String version() {
		String result = BenchmarkApplication.class.getPackage().getImplementationVersion();
		return result!=null ? result : "unknown";
	}
	
	public static String prefixToName(String prefix) {
		StringBuilder builder = new StringBuilder();
		
		for (String segment : prefix.split("\\.")) {
			if (segment.isEmpty())
				continue;
			builder.append(Character.toUpperCase(segment.charAt(0)));
			builder.append(segment, 1, segment.length());
		}
			
		return builder.toString();
	}
	
	public void frameworkOptions(Options options) {
//		Option optionFrameworkActor4j = Option.builder("actor4j").desc("using the Actor4j framework").argName("actor4j").get();
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
	
	protected void printHelp(Options options) {
		try {
			HelpFormatter.builder().setShowSince(false).get().printHelp("benchmark",
				"Benchmark Application for Actor4j. Options:", options,
				"For more details, see https://github.com/relvaner/actor4j", true);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	// for main methods of launchers, so that scripts notice a failed run
	public void runAndExit(String[] args) {
		System.exit(run(args));
	}
	
	public int run(String[] args) {
		Options options = new Options();
		
		frameworkOptions(options);
		
		Option optionClass = Option.builder("class").hasArg().desc("the class name prefix").argName("class").get();
		Option optionActors = Option.builder("actors").hasArg().desc("actors per thread").argName("actors").get();

		Option optionWarmupIterations = Option.builder("warmup").hasArg().desc("the warmup iterations for the benchmark").argName("warmup").get();
		Option optionIterations = Option.builder("iters").hasArg().desc("the benchmark iterations").argName("iters").get();
		Option optionWarmupTime = Option.builder("warmupTime").hasArg().desc("the duration of a warmup iteration in ms").argName("ms").get();
		Option optionMeasurementTime = Option.builder("time").hasArg().desc("the duration of a benchmark iteration in ms").argName("ms").get();
		
		Option optionThreadMode = Option.builder("mode").hasArg().desc("the thread mode").argName("mode").get();
		Option optionParallelismMin = Option.builder("threads").hasArg().desc("the number of threads").argName("threads").get();
		Option optionParallelismFactor = Option.builder("factor").hasArg().desc("the parallelism factor").argName("factor").get();
		Option optionThroughput = Option.builder("throughput").hasArg().desc("the throughput").argName("throughput").get();
		
		Option optionParam1 = Option.builder("param1").hasArg().desc("the first parameter").argName("param1").get();
		Option optionParam2 = Option.builder("param2").hasArg().desc("the second parameter").argName("param2").get();
		
		options.addOption("?", "help", false, "print this message");
		options.addOption("version", false, "print the version information and exit");
				
		options.addOption(optionClass);
		options.addOption(optionActors);
		options.addOption(optionWarmupIterations);
		options.addOption(optionIterations);
		options.addOption(optionWarmupTime);
		options.addOption(optionMeasurementTime);
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
				printHelp(options);
				return 0;
			} else if (line.hasOption("version")) {
				System.out.printf("benchmark version \"%s\"%n", VERSION);
				return 0;
			}
			
			String framework = frameworkClasspath(line);
			if (framework==null) {
				System.err.println("Missing framework option (e.g. -actor4j)");
				printHelp(options);
				return 1;
			}
			if (!line.hasOption("class")) {
				System.err.println("Missing option -class (e.g. -class ping.pong)");
				printHelp(options);
				return 1;
			}
			
			BenchmarkConfig config = new BenchmarkConfig();
			
			if (line.hasOption("actors"))
				config.numberOfActors(Integer.valueOf(line.getOptionValue("actors")));
			if (line.hasOption("warmup"))
				config.warmupIterations(Integer.valueOf(line.getOptionValue("warmup")));
			if (line.hasOption("iters"))
				config.iterations(Integer.valueOf(line.getOptionValue("iters")));
			if (line.hasOption("warmupTime"))
				config.warmupTime(Long.valueOf(line.getOptionValue("warmupTime")));
			if (line.hasOption("time"))
				config.measurementTime(Long.valueOf(line.getOptionValue("time")));
			if (line.hasOption("mode"))
				config.threadMode(line.getOptionValue("mode"));
			if (line.hasOption("threads"))
				config.parallelism(Integer.valueOf(line.getOptionValue("threads")));
			if (line.hasOption("factor"))
				config.parallelismFactor(Integer.valueOf(line.getOptionValue("factor")));
			if (line.hasOption("throughput"))
				config.throughput(Integer.valueOf(line.getOptionValue("throughput")));
			if (line.hasOption("param1"))
				config.param1(line.getOptionValue("param1"));
			if (line.hasOption("param2"))
				config.param2(line.getOptionValue("param2"));
			
			try {
				Class<?> clazz = Class.forName(framework+line.getOptionValue("class")+".Benchmark"+prefixToName(line.getOptionValue("class")));
				Constructor<?> constructor = clazz.getConstructor(BenchmarkConfig.class);
				constructor.newInstance(config);
			} catch (ClassNotFoundException e) {
				System.err.printf("Benchmark not found: %s%n", e.getMessage());
				return 1;
			} catch (InvocationTargetException e) {
				if (e.getCause() instanceof IllegalArgumentException)
					System.err.println(e.getCause().getMessage());
				else
					e.getCause().printStackTrace(); // the benchmark itself failed (it runs in the constructor)
				return 1;
			} catch (ReflectiveOperationException e) {
				e.printStackTrace();
				return 1;
			}
			return 0;
			
		} catch (ParseException | NumberFormatException e) {
			System.err.println("Invalid arguments: " + e.getMessage());
			return 1;
		}
	}
}
