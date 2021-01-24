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

public class Outcome {
	
	Object actual;
	String message;
	
	public Outcome(Object actual) {
		this(null, actual);
	}
	
	public Outcome(String message, Object actual) {
		this.message = message;
		this.actual = actual;
	}
	
	public void shouldBe(boolean expected) {
		assertEquals(message, expected, actual);
	}
	
	public void shouldNotBe(boolean unexpected) {
		shouldBe(!unexpected);
	}
	
	public void shouldBeTrue() {
		shouldBe(true);
	}
	
	public void shouldBeFalse() {
		shouldBe(false);
	}
	
	public void shouldBeEqual(boolean expected) {
		shouldBe(expected);
	}
	
	public void shouldBeNotEqual(boolean unexpected) {
		shouldNotBe(unexpected);
	}
	
	public void shouldBe(Object expected) {
		assertEquals(message, expected, actual);
	}
	
	public void shouldNotBe(Object unexpected) {
		assertNotSame(message, unexpected, actual);
	}
	
	public void shouldBe(Object[] expecteds) {
		assertArrayEquals(message, expecteds, (Object[])actual);
	}
	
	public void shouldBeNull() {
		assertNull(message, actual);
	}
	
	public void shouldBeNotNull() {
		assertNotNull(message, actual);
	}
	
	public void shouldBe(long expected) {
		assertEquals(message, expected, (long)actual);
	}
	
	public void shouldNotBe(long unexpected) {
		assertNotEquals(message, unexpected, (long)actual);
	}
	
	public void shouldBe(String expected) {
		assertEquals(message, expected, actual);
	}
	
	public void shouldNotBe(String unexpected) {
		assertNotEquals(message, unexpected, actual);
	}
	
	public void shouldBe(double expected, double delta) {
		assertEquals(message, expected, (double)actual, delta);
	}
	
	public void shouldNotBe(double unexpected, double delta) {
		assertNotEquals(message, unexpected, (double)actual, delta);
	}
}
