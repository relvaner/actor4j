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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

// launcher with main for the fork test (the forks start this class in a new JVM)
public class ForkSampleLauncher extends BenchmarkApplication {
	@Override
	public void frameworkOptions(Options options) {
		options.addOption(Option.builder("sample").get());
	}
	
	@Override
	public String frameworkClasspath(CommandLine line) {
		return line.hasOption("sample") ? "io.actor4j.benchmark.cli.sample." : null;
	}
	
	public static void main(String[] args) {
		new ForkSampleLauncher().runAndExit(args);
	}
}
