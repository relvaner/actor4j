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
package io.actor4j.benchmark.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * The name of the CPU as the operating system reports it (the brand string of the processor), for the header and
 * the result file: only shown, so that results can be told apart later. Read once per JVM:
 * <ul>
 * <li>Linux: {@code model name} from /proc/cpuinfo</li>
 * <li>Windows: {@code ProcessorNameString} from the registry ({@code reg query})</li>
 * <li>macOS: {@code sysctl -n machdep.cpu.brand_string}</li>
 * </ul>
 * "unknown" if none of them works.
 */
public final class CpuName {
	public static final String UNKNOWN = "unknown";

	private static volatile String name;

	private CpuName() {
	}

	public static String get() {
		String result = name;
		if (result==null) {
			result = detect();
			name = result;
		}
		return result;
	}

	static String detect() {
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		String result = null;
		try {
			if (os.contains("linux"))
				result = fromCpuInfo(Files.readAllLines(Path.of("/proc/cpuinfo")));
			else if (os.contains("windows"))
				result = fromRegQuery(run("reg", "query", "HKLM\\HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0", "/v", "ProcessorNameString"));
			else if (os.contains("mac"))
				result = run("sysctl", "-n", "machdep.cpu.brand_string");
		}
		catch (IOException | RuntimeException e) {
			result = null;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			result = null;
		}
		return result!=null && !result.isBlank() ? clean(result) : UNKNOWN;
	}

	// "model name : <name>", the first one (all CPUs of a socket have the same), ARM: "Model" or "Hardware"
	static String fromCpuInfo(List<String> lines) {
		for (String key : new String[] {"model name", "Model", "Hardware"})
			for (String line : lines) {
				int colon = line.indexOf(':');
				if (colon>0 && line.substring(0, colon).trim().equals(key))
					return line.substring(colon+1).trim();
			}
		return null;
	}

	// "    ProcessorNameString    REG_SZ    <name>"
	static String fromRegQuery(String output) {
		if (output==null)
			return null;
		for (String line : output.split("\\R")) {
			int type = line.indexOf("REG_SZ");
			if (line.contains("ProcessorNameString") && type>=0)
				return line.substring(type+"REG_SZ".length()).trim();
		}
		return null;
	}

	// several spaces in one (some firmware pads the name)
	static String clean(String value) {
		return value.trim().replaceAll("\\s+", " ");
	}

	// the output of a short command, null on failure or after 5 s
	private static String run(String... command) throws IOException, InterruptedException {
		Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
		StringBuilder output = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine())!=null)
				output.append(line).append('\n');
		}
		if (!process.waitFor(5, TimeUnit.SECONDS)) {
			process.destroyForcibly();
			return null;
		}
		return process.exitValue()==0 ? output.toString().trim() : null;
	}
}
