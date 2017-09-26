package redis.unit;

import org.junit.Before;
import org.junit.Test;
import redis.LRULoadingCache;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by bchen on 9/24/17.
 */
public final class LRULoadingCacheTests {

    private ValueProvider valueProvider;
    private Function<String, String> loadingFunc;
    private LRULoadingCache cache;

    @Before
    public void before() {
        this.valueProvider = new ValueProvider();
        this.loadingFunc = (key) -> valueProvider.get(key);
    }

    @Test
    public void testGet() {
        cache = new LRULoadingCache(1, 5000, loadingFunc);
        cache.get("key", "key");
        cache.get("key", "key");

        assertThat(valueProvider.count("key")).isEqualTo(1);
    }

    @Test
    public void testLeastRecentlyUsedEviction() throws Exception {
        cache = new LRULoadingCache(2, 5000, loadingFunc);

        // get key1 and key2
        cache.get("key1", "key1");
        assertThat(valueProvider.count("key1")).isEqualTo(1);
        Thread.sleep(10L);

        cache.get("key2", "key2");
        assertThat(valueProvider.count("key2")).isEqualTo(1);
        Thread.sleep(10L);

        // key1 is cached
        cache.get("key1", "key1");
        assertThat(valueProvider.count("key1")).isEqualTo(1);
        Thread.sleep(10L);

        // get key3
        cache.get("key3", "key3");
        assertThat(valueProvider.count("key3")).isEqualTo(1);
        Thread.sleep(10L);

        // key2 should have got evicted, but key1 still cached
        cache.get("key1", "key1");
        assertThat(valueProvider.count("key1")).isEqualTo(1);
    }

    @Test
    public void testExpiry() throws Exception {
        cache = new LRULoadingCache(2, 500, loadingFunc);

        // get key1 and key2
        cache.get("key1", "key1");
        Thread.sleep(10);
        cache.get("key2", "key2");

        Thread.sleep(2000);

        // get key1 and key2 again, they should both have been evicted
        cache.get("key1", "key1");
        cache.get("key2", "key2");

        assertThat(valueProvider.count("key1")).isEqualTo(2);
        assertThat(valueProvider.count("key2")).isEqualTo(2);
    }

    private static class ValueProvider {
        Map<String, Integer> getCounter;

        public ValueProvider() {
            this.getCounter = new HashMap<>();
        }

        public String get(String key) {
            Integer count = getCounter.get(key);
            getCounter.put(key, count == null ? 1 : count + 1);
            return "haha";
        }

        public Integer count(String key) {
            Integer count = getCounter.get(key);
            return count == null ? 0 : count;
        }
    }

}
