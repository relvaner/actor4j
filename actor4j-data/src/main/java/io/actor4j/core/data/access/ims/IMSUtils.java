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

// collectionName not used; filter, update not implemented 
public final class IMSUtils {
	protected static <K, V> void put(K key, V value, IMS<K, V> imdb, String collectionName) {
		imdb.put(key, value);
	}
	
	public static <K, V> boolean hasOne(K key, String filter, IMS<K, V> imdb, String collectionName) {
		boolean result = false;
		
		if (key!=null)
			result = imdb.getData().get(key) != null;
		
		return result;
	}
	
	public static <K, V> void updateOne(K key, String filter, String update, IMS<K, V> imdb, String collectionName) {
		// not implemented
	}
	
	public static <K, V> void insertOne(K key, V value, IMS<K, V> imdb, String collectionName) {
		put(key, value, imdb, collectionName);
	}
	
	public static <K, V> void replaceOne(K key, String filter, V value, IMS<K, V> imdb, String collectionName) {
		put(key, value, imdb, collectionName);
	}
	
	public static <K, V> void deleteOne(K key, String filter, IMS<K, V> imdb, String collectionName) {
		if (key!=null)
			imdb.remove(key);
	}
	
	public static <K, V> V findOne(K key, String filter, IMS<K, V> imdb, String collectionName) {
		V result = null;
		
		if (key!=null)
			result = imdb.getData().get(key);
		
		return result;
	}
}
