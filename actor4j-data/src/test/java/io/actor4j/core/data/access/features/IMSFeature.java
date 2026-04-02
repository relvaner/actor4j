/*
 * Copyright (c) 2015-2018, David A. Bauer. All rights reserved.
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
package io.actor4j.core.data.access.features;

import org.junit.Test;

import io.actor4j.core.data.access.ims.IMS;
import io.actor4j.core.data.access.ims.IMSIndex;

import static org.junit.Assert.*;

public class IMSFeature {
	@Test(timeout=5000)
	public void test_basic() {
		IMS<String, String> imdb = new IMS<>();
		imdb.getData().put("1", "Insel");
		imdb.getData().put("2", "Sonne");
		imdb.getData().put("3", "Erde");
		imdb.getData().put("4", "Mond");
		
		IMSIndex<String, String> soundex = new IMSIndex<>("soundex");
		soundex
			.create((k, v) -> v.toUpperCase())
			.get()
			.set()
			.insertData((k, v) -> v.toUpperCase())
			.reduce((index_key, set) -> set.size(), (x, y) -> (int)x+(int)y);
		
		imdb.create(soundex);
		assertEquals(4, soundex.idxReduce.get().get());
		imdb.getData().put("5", "Kuchen");
		soundex.insertToIdx.accept("5", "Kuchen");
		assertEquals("Erde", soundex.idxGet.apply(imdb.getData(), "ERDE").findFirst().get());
		assertEquals(5, soundex.idxReduce.get().get());
		imdb.getData().put("6", "Kuchen");
		soundex.insertToIdx.accept("6", "Kuchen");
		assertEquals(6, soundex.idxReduce.get().get());
		assertEquals("Kuchen", soundex.idxGet.apply(imdb.getData(), "KUCHEN").findFirst().get());
		assertEquals(2, soundex.idxGet.apply(imdb.getData(), "KUCHEN").count());
		assertEquals("Kuchen", soundex.idxGet.apply(imdb.getData(), "KUCHEN").skip(1).findFirst().get());
	}
	
	@Test(timeout=5000)
	public void test_more() {
		IMS<String, String> imdb = new IMS<>();
		imdb.getData().put("1", "Insel");
		imdb.getData().put("2", "Sonne");
		imdb.getData().put("3", "Erde");
		imdb.getData().put("4", "Mond");
		
		IMSIndex<String, String> soundex = new IMSIndex<>("soundex");
		soundex
			.create((k, v) -> v.toUpperCase())
			.get()
			.set()
			.remove()
			.reduce((index_key, set) -> set.size(), (x, y) -> (int)x+(int)y)
			.syncData((k, v) -> v.toUpperCase());
//			.insertData((k, v) -> v.toUpperCase())
//			.removeData((k, v) -> v.toUpperCase());
		
		imdb.add(soundex);
		imdb.create(soundex);
		
		assertEquals(4, soundex.idxReduce.get().get());
		imdb.put("5", "Kuchen");
		assertEquals("Erde", soundex.idxGet.apply(imdb.getData(), "ERDE").findFirst().get());
		assertEquals(5, soundex.idxReduce.get().get());
		imdb.put("6", "Kuchen");
		imdb.put("6", "Kuchen");
		assertEquals(6, soundex.idxReduce.get().get());
		assertEquals("Kuchen", soundex.idxGet.apply(imdb.getData(), "KUCHEN").findFirst().get());
		assertEquals(2, soundex.idxGet.apply(imdb.getData(), "KUCHEN").count());
		assertEquals("Kuchen", soundex.idxGet.apply(imdb.getData(), "KUCHEN").skip(1).findFirst().get());
		
		imdb.remove("4");
		assertEquals(5, soundex.idxReduce.get().get());
		soundex.idxRemove.apply("KUCHEN");
		assertEquals(3, soundex.idxReduce.get().get());
	}
}
