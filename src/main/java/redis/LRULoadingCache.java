package redis;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by bchen on 9/24/17.
 */
public class LRULoadingCache {

    private long cacheExpiry;
    private Function<String, String> loadingFunc;
    private LinkedHashMap<String, String> keyToValue;
    private SortedMap<Long, Set<String>> insertionTimeToKeys;
    private final ScheduledExecutorService scheduler;

    public LRULoadingCache(int capacity, long cacheExpiryInMs, Function<String, String> loadingFunc) {
        this.cacheExpiry = cacheExpiryInMs;
        this.loadingFunc = loadingFunc;
        keyToValue = new LRULinkedHashMap<>(capacity, capacity);
        insertionTimeToKeys = new TreeMap<>();
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> evictExpiredEntries(), 1,1, TimeUnit.SECONDS);  // evict expired entries every minute
    }

    public String get(String command, String key) {
        Long currentTimestamp = System.currentTimeMillis();
        String cachedValue;
        synchronized (this) {
            System.out.println("Get " + key + " at " + currentTimestamp);
            cachedValue = keyToValue.get(key);
        }

        if (cachedValue == null) {  // value not cached, go get it
            String value = loadingFunc.apply(command);
            if (value != null) {  // cache non-null value
                synchronized (this) {
                    keyToValue.put(key, value);
                    insertionTimeToKeys.putIfAbsent(currentTimestamp, new HashSet<>());
                    insertionTimeToKeys.get(currentTimestamp).add(key);
                }
            }
            cachedValue = value;
        }

        return cachedValue;
    }

    private synchronized void evictExpiredEntries() {
        Long cutoff = System.currentTimeMillis() - cacheExpiry;
        Set<Long> toEvict = insertionTimeToKeys.headMap(cutoff + 1).keySet().stream().collect(Collectors.toSet());
        for (Long ts : toEvict) {
            for (String key : insertionTimeToKeys.get(ts)) {
                if (keyToValue.containsKey(key)) {
                    keyToValue.remove(key);
                    System.out.println("Evict " + key + " inserted at " + ts);
                }
            }
            insertionTimeToKeys.remove(ts);
        }
    }

    private static class LRULinkedHashMap<K, V> extends LinkedHashMap<K, V> {

        private final int maxEntries;

        LRULinkedHashMap(int initialCapacity, int maxEntries) {
            super(initialCapacity, 0.75f, true);
            this.maxEntries = maxEntries;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
            if (size() > maxEntries) {
                System.out.println("Evict eldest entry");
                return true;
            }
            return false;
        }
    }

}
