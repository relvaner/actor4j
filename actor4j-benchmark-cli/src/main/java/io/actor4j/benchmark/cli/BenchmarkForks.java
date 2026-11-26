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
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import io.actor4j.benchmark.report.BenchmarkOutput;
import io.actor4j.benchmark.report.BenchmarkResult;
import io.actor4j.benchmark.report.Json;
import io.actor4j.benchmark.utils.Timeouts;

// Forks like JMH (-f n): the benchmark runs n times, one JVM after the other (never at the same time), with the
// same arguments and JVM arguments. The results of the forks are merged into one result per benchmark.
public class BenchmarkForks {
	// options handled here, not passed on to the forks
	protected static final Set<String> OWN_OPTIONS = Set.of("-f", "-rf", "-rff", "-jvmArgs");

	protected final Class<?> mainClass;
	// JVM arguments of the forks (-jvmArgs, split at white space), null: those of this JVM
	protected final String jvmArgs;
	// time limit of a fork in ms (from -to), 0: none
	protected final long limitMillis;

	public BenchmarkForks(Class<?> mainClass) {
		this(mainClass, null);
	}

	public BenchmarkForks(Class<?> mainClass, String jvmArgs) {
		this(mainClass, jvmArgs, 0);
	}

	public BenchmarkForks(Class<?> mainClass, String jvmArgs, long limitMillis) {
		super();
		this.mainClass = mainClass;
		this.jvmArgs = jvmArgs;
		this.limitMillis = limitMillis;
	}

	@SuppressWarnings("unchecked")
	public int run(String[] args, int forks, String resultFile) {
		Map<String, List<Map<String, Object>>> results = new LinkedHashMap<>();
		try {
			for (int fork=1; fork<=forks; fork++) {
				System.out.printf(fork>1 ? "%n# Fork: %d of %d%n" : "# Fork: %d of %d%n", fork, forks);
				Path forkFile = Files.createTempFile("benchmark-fork", ".json");
				Process process = null;
				try {
					process = new ProcessBuilder(command(args, forkFile)).inheritIO().start();
					if (limitMillis>0 && !process.waitFor(limitMillis, TimeUnit.MILLISECONDS)) {
						process.destroyForcibly().waitFor();
						System.err.printf("Fork %d did not end within %s and was terminated (-to)%n", fork, Timeouts.format(limitMillis));
						return 1;
					}
					int exitCode = process.waitFor();
					if (exitCode!=0) {
						System.err.printf("Fork %d failed (exit code %d)%n", fork, exitCode);
						return 1;
					}
					String text = Files.readString(forkFile, StandardCharsets.UTF_8);
					if (text.isBlank()) {
						System.err.printf("Fork %d wrote no result (the scenario uses neither Benchmark nor ThroughputBenchmark)%n", fork);
						return 1;
					}
					List<Map<String, Object>> forkResults = new ArrayList<>();
					try {
						for (Object result : (List<Object>)Json.parse(text))
							forkResults.add((Map<String, Object>)result);
					} catch (IllegalArgumentException | ClassCastException e) {
						System.err.printf("Fork %d wrote an invalid result file: %s%n", fork, e.getMessage());
						return 1;
					}
					for (Map<String, Object> result : forkResults)
						results.computeIfAbsent(String.valueOf(result.get("benchmark")), k -> new ArrayList<>()).add(result);
				} catch (InterruptedException e) {
					// do not leave the fork running
					if (process!=null)
						process.destroyForcibly();
					Thread.currentThread().interrupt();
					return 1;
				}
				finally {
					Files.deleteIfExists(forkFile);
				}
			}
		} catch (IOException e) {
			System.err.println("Forks failed: "+e.getMessage());
			return 1;
		}

		List<Map<String, Object>> merged = new ArrayList<>();
		for (List<Map<String, Object>> forkResults : results.values())
			merged.add(BenchmarkResult.merge(forkResults));

		print(merged);
		if (resultFile!=null)
			try {
				Files.writeString(Path.of(resultFile), Json.write(merged), StandardCharsets.UTF_8);
			} catch (IOException e) {
				System.err.println("Result file not written: "+e.getMessage());
				return 1;
			}
		return 0;
	}

	// java, the JVM arguments and class path of this JVM, the main class, the arguments without -f/-rf/-rff
	protected List<String> command(String[] args, Path forkFile) {
		List<String> command = new ArrayList<>();
		command.add(ProcessHandle.current().info().command().orElse(Path.of(System.getProperty("java.home"), "bin", "java").toString()));
		if (jvmArgs!=null)
			command.addAll(splitArgs(jvmArgs));
		else
			for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments())
				if (!isDebugAgent(arg))
					command.add(arg);
		// the manifest entry Enable-Native-Access of an executable jar does not apply with -cp (e.g. thread pinning with FFM)
		if (Runtime.version().feature()>=22 && command.stream().noneMatch(arg -> arg.startsWith("--enable-native-access")))
			command.add("--enable-native-access=ALL-UNNAMED");
		command.add("-cp");
		command.add(System.getProperty("java.class.path"));
		command.add(mainClass.getName());
		for (int i=0; i<args.length; i++) {
			if (OWN_OPTIONS.contains(args[i])) {
				i++; // and its value
				continue;
			}
			int eq = args[i].indexOf('=');
			if (eq>0 && OWN_OPTIONS.contains(args[i].substring(0, eq)))
				continue; // -f=2: the value is in the same argument
			command.add(args[i]);
		}
		command.add("-rff");
		command.add(forkFile.toString());
		return command;
	}

	// JVM arguments split at white space, a part in double quotes stays together (-Dpath="C:\Program Files")
	protected static List<String> splitArgs(String text) {
		List<String> result = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean quoted = false;
		boolean token = false;
		for (char c : text.toCharArray()) {
			if (c=='"') {
				quoted = !quoted;
				token = true;
			}
			else if (Character.isWhitespace(c) && !quoted) {
				if (token)
					result.add(current.toString());
				current.setLength(0);
				token = false;
			}
			else {
				current.append(c);
				token = true;
			}
		}
		if (quoted)
			throw new IllegalArgumentException("Unclosed quote in -jvmArgs: "+text);
		if (token)
			result.add(current.toString());
		return result;
	}

	// a debugger agent of this JVM (e.g. from the IDE) would make each fork fail on the same debug port
	protected static boolean isDebugAgent(String arg) {
		return arg.startsWith("-agentlib:jdwp") || arg.startsWith("-Xrunjdwp") || arg.equals("-Xdebug");
	}

	// result over all forks, like JMH at the end of the forks
	@SuppressWarnings("unchecked")
	protected void print(List<Map<String, Object>> merged) {
		for (Map<String, Object> result : merged) {
			Map<String, Object> metric = (Map<String, Object>)result.get("primaryMetric");
			DescriptiveStatistics statistics = new DescriptiveStatistics();
			for (Object values : (List<Object>)metric.get("rawData"))
				for (Object value : (List<Object>)values)
					statistics.addValue(Json.number(value));
			System.out.print(BenchmarkOutput.result(String.valueOf(result.get("benchmark")), statistics, String.valueOf(metric.get("scoreUnit")),
				"forks: "+result.get("forks")));
		}
	}
}
