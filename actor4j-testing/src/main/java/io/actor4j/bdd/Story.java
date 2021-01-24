/*
 * bdd4j - Framework for behavior-driven development
 * Copyright (c) 2014, David A. Bauer
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */
package io.actor4j.bdd;

import java.util.LinkedList;
import java.util.List;

public class Story {
	
	private List<Scenario> scenarios;
	private List<Examples> examples;
	
	public Story() {
		scenarios = new LinkedList<Scenario>();
		examples = new LinkedList<Examples>();
	}
	
	public Scenario scenario() {
		return scenario(true);
	}
	
	public Scenario scenario(boolean sameExamples) {
		if (sameExamples) {  // bis auf weiteres das gleiche
			if (examples.size()==0)
				examples.add(new Examples());
			else 
				examples.add(examples.get(examples.size()-1));
		}
		else
			examples.add(new Examples()); // für jedes Szenario neu angelegt
		
		Scenario result = new Scenario(examples.get(examples.size()-1));
		examples.get(examples.size()-1).setScenario(result);
		
		scenarios.add(result);
		
		return result;
	}
	
	public void run() {
		int i=0;
		for (Scenario scenario : scenarios) {
			scenario.run(examples.get(i));
			if (i<scenarios.size()-1)
				System.out.println("--");
			i++;
		}
	}
}
