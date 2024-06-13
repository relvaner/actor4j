package io.actor4j.jcache;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public interface AsyncCacheLoader<K, V> {
	CompletableFuture<V> asyncLoad(K key, Executor executor);
	CompletableFuture<Map<K,V>> asyncLoadAll(Iterable<? extends K> keys, Executor executor);
}
