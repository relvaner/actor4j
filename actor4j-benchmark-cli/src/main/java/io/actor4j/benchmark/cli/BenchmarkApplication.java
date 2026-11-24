/*
 * Copyright (c) 2015-2026, David A. Bauer. All rights reserved.
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.help.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import io.actor4j.benchmark.BenchmarkConfig;
import io.actor4j.benchmark.utils.Timeouts;

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
	
	// framework options of a launcher (e.g. -actor4j, -runtime), added to the general options
	public void frameworkOptions(Options options) {
	}

	// framework specific options into the config (e.g. -runtime), called before the scenario is created
	// and before forks; an invalid value: IllegalArgumentException with a message
	public void frameworkConfig(CommandLine line, BenchmarkConfig config) {
	}

	// package prefix of the scenarios for the chosen framework (e.g. "actor4j.benchmark.scenarios."),
	// null: no framework option given
	public String frameworkClasspath(CommandLine line) {
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
	
	protected static String unknownOption(Options options, String[] args) {
		for (int i=0; i<args.length; i++) {
			String arg = args[i];
			if (!arg.startsWith("-") || arg.equals("-") || arg.matches("-\\d.*"))
				continue;
			String name = arg.startsWith("--") ? arg.substring(2) : arg.substring(1);
			int eq = name.indexOf('=');
			if (eq>0) {
				// -wi=3: the value is part of the argument, as commons-cli accepts it
				if (!options.hasOption(name.substring(0, eq)) || !options.getOption(name.substring(0, eq)).hasArg())
					return arg;
				continue;
			}
			if (!options.hasOption(name))
				return arg;
			if (options.getOption(name).hasArg())
				i++; // its value may start with '-' (e.g. -jvmArgs "-Xmx1g")
		}
		return null;
	}
	
	// -wi=3 as -wi 3: commons-cli takes the form only for options with one letter or a long name
	protected static String[] splitEquals(Options options, String[] args) {
		List<String> result = new ArrayList<>();
		for (int i=0; i<args.length; i++) {
			String arg = args[i];
			int eq = arg.indexOf('=');
			if (arg.startsWith("-") && eq>1) {
				String name = arg.substring(arg.startsWith("--") ? 2 : 1, eq);
				if (options.hasOption(name) && options.getOption(name).hasArg()) {
					result.add(arg.substring(0, eq));
					result.add(arg.substring(eq+1));
					continue;
				}
			}
			result.add(arg);
			if (arg.startsWith("-") && options.hasOption(arg.replaceFirst("^--?", "")) && options.getOption(arg.replaceFirst("^--?", "")).hasArg() && i+1<args.length)
				result.add(args[++i]); // a value, kept as it is (e.g. -p name=value)
		}
		return result.toArray(new String[0]);
	}
	
	public int run(String[] args) {
		Options options = new Options();
		
		frameworkOptions(options);
		
		Option optionClass = Option.builder("class").hasArg().desc("the class name prefix").argName("class").get();
		Option optionActors = Option.builder("actors").hasArg().desc("actors per thread").argName("actors").get();

		// iterations and times named like JMH
		Option optionWarmupIterations = Option.builder("wi").hasArg().desc("the warmup iterations").argName("int").get();
		Option optionIterations = Option.builder("i").hasArg().desc("the measurement iterations").argName("int").get();
		Option optionWarmupTime = Option.builder("w").hasArg().desc("the duration of a warmup iteration (e.g. 1s, 500ms)").argName("time").get();
		Option optionMeasurementTime = Option.builder("r").hasArg().desc("the duration of a measurement iteration (e.g. 1s, 500ms)").argName("time").get();
		Option optionTimeout = Option.builder("to").hasArg().desc("the time limit of an iteration, like JMH (default: 10min)").argName("time").get();
		
		Option optionThreadMode = Option.builder("mode").hasArg().desc("the thread mode").argName("mode").get();
		Option optionParallelismMin = Option.builder("threads").hasArg().desc("the number of threads").argName("threads").get();
		Option optionParallelismFactor = Option.builder("factor").hasArg().desc("the parallelism factor").argName("factor").get();
		Option optionThroughput = Option.builder("throughput").hasArg().desc("the throughput").argName("throughput").get();
		
		Option optionParam = Option.builder("p").hasArg().desc("a parameter of the scenario, like JMH (repeatable, e.g. -p number=30)").argName("name=value").get();
		
		options.addOption("?", "help", false, "print this message");
		options.addOption("version", false, "print the version information and exit");
				
		options.addOption(optionClass);
		options.addOption(optionActors);
		options.addOption(optionWarmupIterations);
		options.addOption(optionIterations);
		options.addOption(optionWarmupTime);
		options.addOption(optionMeasurementTime);
		options.addOption(optionTimeout);
		options.addOption(optionThreadMode);
		options.addOption(optionParallelismMin);
		options.addOption(optionParallelismFactor);
		options.addOption(optionThroughput);

		options.addOption(optionParam);
		
		// results in the JSON format of JMH, like JMH's -rf json -rff file
		options.addOption(Option.builder("rf").hasArg().desc("the result format: json (default with -rff)").argName("format").get());
		options.addOption(Option.builder("rff").hasArg().desc("the result file (default with -rf: benchmark-result.json)").argName("file").get());
		// forks like JMH's -f: the benchmark runs n times in its own JVM, one after the other, results merged
		options.addOption(Option.builder("f").hasArg().desc("the number of forks (one JVM after the other, default 0: in this JVM)").argName("int").get());
		options.addOption(Option.builder("jvmArgs").hasArg().desc("the JVM arguments of the forks (default: those of this JVM)").argName("string").get());
		
		// only exact options: commons-cli would read e.g. -warmup as -w with the value "armup"
		String unknown = unknownOption(options, args);
		if (unknown!=null) {
			System.err.printf("Unknown option %s%n", unknown);
			printHelp(options);
			return 1;
		}
		
		CommandLineParser parser = new DefaultParser();

		try {
			CommandLine line = parser.parse(options, splitEquals(options, args));

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
			
			// all options are checked here, also before forks, so that a wrong value fails at once and not in every fork
			BenchmarkConfig config = new BenchmarkConfig();
			
			if (line.hasOption("actors"))
				config.numberOfActors(intOption(line, "actors", 1));
			if (line.hasOption("wi"))
				config.warmupIterations(intOption(line, "wi", 0));
			if (line.hasOption("i"))
				config.iterations(intOption(line, "i", 1));
			if (line.hasOption("w"))
				config.warmupTime(BenchmarkConfig.parseTime(line.getOptionValue("w")));
			if (line.hasOption("r"))
				config.measurementTime(BenchmarkConfig.parseTime(line.getOptionValue("r")));
			if (line.hasOption("mode"))
				config.threadMode(line.getOptionValue("mode"));
			if (line.hasOption("threads"))
				config.parallelism(intOption(line, "threads", 1));
			if (line.hasOption("factor"))
				config.parallelismFactor(intOption(line, "factor", 1));
			if (line.hasOption("throughput"))
				config.throughput(intOption(line, "throughput", 1));
			if (line.hasOption("p"))
				for (String param : line.getOptionValues("p")) {
					int eq = param.indexOf('=');
					if (eq<=0)
						throw new IllegalArgumentException(String.format("Invalid parameter -p %s (expected name=value)", param));
					config.param(param.substring(0, eq).trim(), param.substring(eq+1).trim());
				}
			if (line.hasOption("to"))
				config.timeout(BenchmarkConfig.parseTime(line.getOptionValue("to")));
			config.resultFile(resultFile(line));
			int forks = line.hasOption("f") ? intOption(line, "f", 0) : 0;
			if (forks==0 && line.hasOption("jvmArgs"))
				System.err.println("Warning: -jvmArgs has no effect without -f (it sets the JVM arguments of the forks)");
			
			String className = framework+line.getOptionValue("class")+".Benchmark"+prefixToName(line.getOptionValue("class"));
			config.property("benchmark", className);
			config.property("scenario", line.getOptionValue("class"));
			
			// framework options and the scenario class are checked here too, before forks start a JVM each
			frameworkConfig(line, config);
			Class<?> clazz;
			try {
				clazz = Class.forName(className);
			} catch (ClassNotFoundException e) {
				System.err.printf("Benchmark not found: %s%n", e.getMessage());
				return 1;
			}
			
			if (forks>0) {
				// per fork: all iterations at most -to each, plus a minute for start and shutdown of the JVM
				long limit = (long)(config.warmupIterations()+config.iterations())*config.timeout()+60_000;
				return new BenchmarkForks(getClass(), line.getOptionValue("jvmArgs"), limit).run(args, forks, config.resultFile());
			}
			
			try {
				Constructor<?> constructor = clazz.getConstructor(BenchmarkConfig.class);
				constructor.newInstance(config);
			} catch (InvocationTargetException e) {
				Timeouts.TimeoutException timeout = timeout(e.getCause());
				if (timeout!=null)
					System.err.println(timeout.getMessage()); // with the stack of the hanging thread, the own stack says nothing
				else if (e.getCause() instanceof IllegalArgumentException)
					System.err.println(e.getCause().getMessage());
				else
					e.getCause().printStackTrace(); // the benchmark itself failed (it runs in the constructor)
				return 1;
			} catch (ReflectiveOperationException e) {
				e.printStackTrace();
				return 1;
			}
			return 0;
			
		} catch (ParseException e) {
			System.err.println("Invalid arguments: " + e.getMessage());
			return 1;
		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
			return 1;
		}
	}
	
	// a timeout as the failure itself or as its cause (a scenario may wrap it)
	protected static Timeouts.TimeoutException timeout(Throwable failure) {
		for (Throwable t = failure; t!=null; t = t.getCause())
			if (t instanceof Timeouts.TimeoutException timeout)
				return timeout;
		return null;
	}
	
	// an integer option with a lower bound, e.g. -threads at least 1
	protected static int intOption(CommandLine line, String name, int min) {
		String value = line.getOptionValue(name);
		int result;
		try {
			result = Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(String.format("Invalid number for -%s: '%s'", name, value));
		}
		if (result<min)
			throw new IllegalArgumentException(String.format("-%s must be at least %d: %d", name, min, result));
		return result;
	}
	
	// result file of -rf/-rff (null without both), checked before the run: otherwise a wrong path fails only at the end
	protected static String resultFile(CommandLine line) {
		if (!line.hasOption("rf") && !line.hasOption("rff"))
			return null;
		String format = line.getOptionValue("rf", "json");
		if (!format.equalsIgnoreCase("json"))
			throw new IllegalArgumentException(String.format("Unknown result format -rf %s (allowed: json)", format));
		String file = line.getOptionValue("rff", "benchmark-result.json");
		Path path = Path.of(file).toAbsolutePath();
		Path directory = path.getParent();
		if (Files.isDirectory(path) || directory==null || !Files.isDirectory(directory) || !Files.isWritable(directory)
				|| (Files.exists(path) && !Files.isWritable(path)))
			throw new IllegalArgumentException(String.format("Result file not writable: %s", path));
		return file;
	}
}
