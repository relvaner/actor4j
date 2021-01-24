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

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;

public class Scenario {
	
	private List<Step> steps;
	private List<String> annotations;
	
	private static Examples examples;
	private static int iteration;
	
	@SuppressWarnings("static-access")
	public Scenario(final Examples examples) {
		this.examples = examples;
		
		steps = new LinkedList<>();
		annotations = new LinkedList<>();
	}
	
	public Examples example(Object... preconditions) {
		return examples.addPreconditions(preconditions);
	}
	
	public static Object[] example() {
		return examples.getPreconditions(iteration);
	}
	
	public static Object[] captured() {
		return examples.getPostconditions(iteration);
	}
	
	public Scenario given(Step step) {
		steps.add(step);
		
		return this;
	}
	
	public Scenario when(Step step) {
		steps.add(step);
		
		return this;
	}
	
	public Scenario then(Step step) {
		steps.add(step);
		
		return this;
	}
	
	public Scenario and(Step step) {
		steps.add(step);
		
		return this;
	}
	
	@SuppressWarnings("static-access")
	public Scenario run(final Examples examples) {
		this.examples = examples;
		
		if (examples.getSize()==0)
			iteration=-1;
		else
			iteration=0;
		
		for (;iteration<examples.getSize(); iteration++) {
			int i=0;
			for (Step step : steps) {
				if ( !annotations.isEmpty() && (step instanceof Annotation) ) {
					System.out.println(annotations.get(i));
					i++;
				}
				else
					step.step();
			}
			
			if (examples.getSize()!=0 && iteration<examples.getSize()-1)
				System.out.println("--");
		}
		
		return this;
	}
	
	public Scenario annotate(String text) {
		steps.add(new Annotation() {
			@Override
			public void step() {
			}
		});
		annotations.add(text);
		
		return this;
	}
	
	public static <E extends Exception> Step ensureThrows(final Step step, final Class<E> c) {
		Step result = new Step() {
			@Override
			public void step() {
				boolean thrown = false;
				
				try {
					step.step();
				}
				catch(Exception e) {
					if (e.getClass().equals(c))
						thrown = true;
				}
				
				assertTrue("Expected exception: "+c.getName(), thrown);
			}
		};
		
		return result;
	}
}
