package actor4j.web.websocket.features;

import static org.junit.Assert.*;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class GuavaCacheFeature {
	@Test
	public void test() {
		final int[] values = new int[] { 341, 351, 451, 318, 292, 481, 240, 478, 382, 502, 158, 401, 438, 353, 165, 344, 6, 9, 18, 31, 77, 90, 45, 63, 190, 1 };
		AtomicInteger iteration = new AtomicInteger(0);
		
		LoadingCache<Integer, Integer> cache = CacheBuilder.newBuilder()
				.maximumSize(500)
				.concurrencyLevel(1)
				.build(new CacheLoader<Integer, Integer>() {
					@Override
					public Integer load(Integer key) throws Exception {
						int found = -1;
						
						if (key<values.length)
							found = values[key];
						
						iteration.incrementAndGet();
						
						return found;
					}
				});
		
		for (int i=0; i<values.length; i++)
			assertEquals(values[i], (int)cache.getUnchecked(i));
		for (int i=0; i<values.length; i++)
			assertEquals(values[i], (int)cache.getUnchecked(i));
		
		assertEquals(-1, (int)cache.getUnchecked(255));
		assertEquals(-1, (int)cache.getUnchecked(2568));
		
		try {
			assertEquals(-2, (int)cache.get(187, new Callable<Integer>() {
				@Override
				public Integer call() throws Exception {
					return -2;
				}
			}));
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		
		assertEquals(values.length+2, iteration.get());
	}
}
