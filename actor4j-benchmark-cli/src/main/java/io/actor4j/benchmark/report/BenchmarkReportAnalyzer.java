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
package io.actor4j.benchmark.report;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

// Summarizes several runs (separate JVMs) of the same scenarios from one report file.
// Runs are grouped by their header (#actors, activeThreads, benchmark name), not by count.
public class BenchmarkReportAnalyzer {
	
	public static void main(String[] args) throws IOException {
		Path input  = Path.of(args.length>0 ? args[0] : "report.txt");
		Path output = Path.of(args.length>1 ? args[1] : "report.stats.txt");
		
		try (BufferedReader reader = Files.newBufferedReader(input); BufferedWriter writer = Files.newBufferedWriter(output)) {
			analyze(reader, writer);
		}
	}
	
	public static void analyze(Reader input, Writer output) throws IOException {
		Map<String, DescriptiveStatistics> groups = new LinkedHashMap<>();
		
		String countActors   = "";
		String activeThreads = "";
		String benchmarkName = "";
		String runtime       = "";
		BufferedReader reader = new BufferedReader(input);
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.startsWith("#actors"))
				countActors = line;
			else if (line.startsWith("activeThreads"))
				activeThreads = line;
			else if (line.startsWith("runtime"))
				runtime = line;
			else if (line.startsWith("Benchmark started"))
				benchmarkName = line;
			else if (line.startsWith("statistics::mean ")) {
				String header = String.join("\n", Stream.of(countActors, activeThreads, runtime, benchmarkName).filter(h -> !h.isEmpty()).toList());
				groups.computeIfAbsent(header, k -> new DescriptiveStatistics()).addValue(parseNumber(line));
			}
		}
		
		DecimalFormat decimalFormat = new DecimalFormat("###,###,###,##0.###");
		for (Map.Entry<String, DescriptiveStatistics> entry : groups.entrySet()) {
			DescriptiveStatistics statistics = entry.getValue();
			output.write(entry.getKey()+"\n");
			output.write("runs   : "+statistics.getN()+"\n");
			output.write("mean   : "+decimalFormat.format(statistics.getMean())+"\n");
			output.write("sd     : "+decimalFormat.format(statistics.getStandardDeviation())+"\n");
			output.write("median : "+decimalFormat.format(statistics.getPercentile(50))+"\n");
			output.write("\n");
		}
		output.flush();
	}
	
	// value after the colon, e.g. "statistics::mean : 1,234.5 ms", in the default locale like the
	// DecimalFormat that wrote it
	protected static double parseNumber(String line) {
		String value = line.substring(line.lastIndexOf(':')+1).trim();
		try {
			return NumberFormat.getInstance().parse(value).doubleValue();
		} catch (ParseException e) {
			throw new IllegalArgumentException("No number in line: "+line, e);
		}
	}
}
