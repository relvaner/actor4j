package io.actor4j.benchmark.report;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class BenchmarkReportAnalyzer {
	public static void main(String[] args) {
		try {
			final int ITERATIONS = 10; //50;
			DecimalFormat decimalFormat = new DecimalFormat("###,###,###,###");
			
		    String current_countActors    = "";
		    String current_activeThreads  = "";
		    String current_benchmarkName  = "";
		    String countActors    = "";
		    String activeThreads  = "";
		    String benchmarkName  = "";
		    int i = 0;
		    /*
		    BufferedReader reader = new BufferedReader(new FileReader("report.ppgb.txt"));
		    BufferedWriter writer = new BufferedWriter(new FileWriter("report.ppgb.stats.txt"));
		    */
		    BufferedReader reader = new BufferedReader(new FileReader("report.txt"));
		    BufferedWriter writer = new BufferedWriter(new FileWriter("report.stats.txt"));
		    String line = null;
		    DescriptiveStatistics statistics = new DescriptiveStatistics();
		    while ((line = reader.readLine()) != null) {
		    	if (line.startsWith("#actors"))
		    		countActors = line;
		    	else if (line.startsWith("activeThreads"))
		    		activeThreads = line;
		    	else if (line.startsWith("Benchmark started"))
		    		benchmarkName = line;
		    	else if (line.startsWith("statistics::mean ")) {
		    		statistics.addValue(Double.valueOf(line.replaceAll("\\D+","")));
		    		if (i==ITERATIONS-1) {
		    			i = -1;
		    			writer.write(current_countActors);writer.newLine();
		    			writer.write(current_activeThreads);writer.newLine();
		    			writer.write(current_benchmarkName);writer.newLine();
		    			writer.write(decimalFormat.format(statistics.getMean()));writer.newLine();
		    			writer.write(decimalFormat.format(statistics.getStandardDeviation()));writer.newLine();
		    			writer.write(decimalFormat.format(statistics.getPercentile(50)));writer.newLine();
		    			writer.newLine();
		    			statistics = new DescriptiveStatistics();
		    		}
		    		else if (i==0) {
		    			current_countActors = countActors;
		    			current_activeThreads = activeThreads;
		    			current_benchmarkName = benchmarkName;
		    		}
		    		else {
		    			assert(current_countActors.equals(countActors));
		    			assert(current_activeThreads.equals(activeThreads));
		    			assert(current_benchmarkName.equals(benchmarkName));
		    		}
		    		i++;
		    	}
		    		
		    }
		    reader.close();
		    writer.close();
		} 
		catch (IOException e) {
		    e.printStackTrace();
		}
	}
}
