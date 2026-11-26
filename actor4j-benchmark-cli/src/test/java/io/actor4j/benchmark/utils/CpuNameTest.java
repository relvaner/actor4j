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

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

public class CpuNameTest {
	@Test
	public void test_cpuinfo() {
		List<String> lines = List.of(
			"processor\t: 0",
			"vendor_id\t: SomeVendor",
			"model name\t: Some CPU 16-Core Processor   ",
			"processor\t: 1",
			"model name\t: Some CPU 16-Core Processor");
		assertEquals("Some CPU 16-Core Processor", CpuName.fromCpuInfo(lines));
	}

	@Test
	public void test_cpuinfo_arm() {
		// ARM: no "model name", but "Model"
		assertEquals("Some Board Rev 1.0", CpuName.fromCpuInfo(List.of("processor\t: 0", "Model\t\t: Some Board Rev 1.0")));
		assertNull(CpuName.fromCpuInfo(List.of("processor\t: 0")));
	}

	@Test
	public void test_reg_query() {
		String output = "HKEY_LOCAL_MACHINE\\HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0\n"
			+"    ProcessorNameString    REG_SZ    Some CPU with Graphics        \n";
		assertEquals("Some CPU with Graphics", CpuName.fromRegQuery(output));
		assertNull(CpuName.fromRegQuery("nothing"));
		assertNull(CpuName.fromRegQuery(null));
	}

	@Test
	public void test_clean() {
		assertEquals("Some CPU 8-Core", CpuName.clean("  Some   CPU\t8-Core  "));
	}

	@Test
	public void test_get() {
		// on every machine a name, at least "unknown", the same each time
		String name = CpuName.get();
		System.out.printf("CpuNameTest: %s%n", name);
		assertNotNull(name);
		assertFalse(name.isBlank());
		assertSame(name, CpuName.get());
	}
}
