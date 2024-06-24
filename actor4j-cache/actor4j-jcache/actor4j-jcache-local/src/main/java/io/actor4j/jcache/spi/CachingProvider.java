package io.actor4j.jcache.spi;

import io.actor4j.core.pods.api.Caching;

public class CachingProvider implements Caching<LocalActorCacheManager> {
	private final LocalActorCacheManager cacheManager;
	
	// Initialization-on-demand holder idiom
	private static class LazyHolder {
		private final static CachingProvider INSTANCE = new CachingProvider();
	}
	
	private CachingProvider() {
		cacheManager = new LocalActorCacheManager();
	}
	
	public static CachingProvider getCachingProvider() {
		return LazyHolder.INSTANCE;
	}
	
	@Override
	public LocalActorCacheManager getCacheManager() {
		return cacheManager;
	}
}
