package redis;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by bchen on 9/24/17.
 */
public class LRULoadingCache {

    private int maxCapacity;
    private long cacheExpiry;
    private int currentCapacity;
    private Function<String, String> loadingFunc;
    private Map<String, String> keyToValue;
    private SortedMap<Long, Set<String>> insertionTimeToKeys;
    private Map<String, Long> keyToLastUsedTime;
    private final ScheduledExecutorService scheduler;

    public LRULoadingCache(int capacity, long cacheExpiryInMs, Function<String, String> loadingFunc) {
        this.maxCapacity = capacity;
        this.currentCapacity = 0;
        this.cacheExpiry = cacheExpiryInMs;
        this.loadingFunc = loadingFunc;
        keyToValue = new ConcurrentHashMap<>(capacity);
        insertionTimeToKeys = new TreeMap<>();
        keyToLastUsedTime = new HashMap<>();
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
                    if (currentCapacity == maxCapacity) {
                        evictLeastRecentlyUsed();
                    }
                    keyToValue.put(key, value);
                    currentCapacity++;
                    insertionTimeToKeys.putIfAbsent(currentTimestamp, new HashSet<>());
                    insertionTimeToKeys.get(currentTimestamp).add(key);
                    keyToLastUsedTime.put(key, currentTimestamp);
                }
            }
            cachedValue = value;
        } else {
            synchronized (this) {
                keyToLastUsedTime.put(key, currentTimestamp);
            }
        }

        return cachedValue;
    }

    private void evictLeastRecentlyUsed() {
        Long lruTime = Long.MAX_VALUE;
        String lruKey = null;
        for (Map.Entry<String, Long> entry : keyToLastUsedTime.entrySet()) {
            if (entry.getValue() < lruTime) {
                lruTime = entry.getValue();
                lruKey = entry.getKey();
            }
        }
        keyToValue.remove(lruKey);
        keyToLastUsedTime.remove(lruKey);
        currentCapacity--;
        System.out.println("Evict " + lruKey + " last used at " + lruTime);
    }

    private synchronized void evictExpiredEntries() {
        Long cutoff = System.currentTimeMillis() - cacheExpiry;
        Set<Long> toEvict = insertionTimeToKeys.headMap(cutoff + 1).keySet().stream().collect(Collectors.toSet());
        for (Long ts : toEvict) {
            for (String key : insertionTimeToKeys.get(ts)) {
                if (keyToValue.containsKey(key)) {
                    keyToValue.remove(key);
                    keyToLastUsedTime.remove(key);
                    currentCapacity--;
                    System.out.println("Evict " + key + " inserted at " + ts);
                }
            }
            insertionTimeToKeys.remove(ts);
        }
    }

}
