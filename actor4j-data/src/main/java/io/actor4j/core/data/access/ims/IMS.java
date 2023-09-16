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
package io.actor4j.core.data.access.ims;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

// In-Memory Storage (IMS)
public class IMS<K, V> {
	protected Map<K, V> data;
	protected Map<String, IMSIndex<K, V>> indexMap;

	public IMS() {
		data = new HashMap<>();
		indexMap = new HashMap<>();
	}
	
	public Map<K, V> getData() {
		return data;
	}
	
	public void setData(Map<K, V> data) {
		this.data = data;
	}

	public Map<String, IMSIndex<K, V>> getIndexMap() {
		return indexMap;
	}

	public void setIndexMap(Map<String, IMSIndex<K, V>> indexMap) {
		this.indexMap = indexMap;
	}

	public void create(IMSIndex<K, V> indexObject) {
		indexObject.map = indexObject.create.apply(data);
	}
	
	public void add(IMSIndex<K, V> indexObject) {
		indexMap.put(indexObject.name, indexObject);
	}
	
	public void put(K key, V value, IMSIndex<K, V> indexObject) {
		data.put(key, value);
		if (indexObject.setd!=null)
			indexObject.setd.accept(key, value);
	}
	
	public void remove(K key, IMSIndex<K, V> indexObject) {
		if (indexObject.removed!=null)
			indexObject.removed.accept(key, data.get(key));
		
		data.remove(key);
	}
	
	public void put(K key, V value) {
		data.put(key, value);
		
		Iterator<Entry<String, IMSIndex<K, V>>> iterator = indexMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, IMSIndex<K, V>> entry = iterator.next();
			if (entry.getValue().setd!=null)
				entry.getValue().setd.accept(key, value);
		}
	}
	
	public void remove(K key) {
		Iterator<Entry<String, IMSIndex<K, V>>> iterator = indexMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, IMSIndex<K, V>> entry = iterator.next();
			if (entry.getValue().removed!=null)
				entry.getValue().removed.accept(key, data.get(key));
		}
		
		data.remove(key);
	}
}
