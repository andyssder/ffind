package io.github.andyssder.ffind.cache;

import io.github.andyssder.ffind.model.idea.CopyUsageInfo;
import java.lang.ref.SoftReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FindResultCache {

    private static final FindResultCache INSTANCE = new FindResultCache();

    // cache map
    private final Map<String, SoftReference<CachedResult>> cache = new ConcurrentHashMap<>();

    private FindResultCache() {

    }

    public static FindResultCache getInstance() {
        return INSTANCE;
    }

    /**
     * Returns cached find result  for the specified field.
     *
     * @param key cache key
     * @return cached result if exist or empty.
     */
    public Optional<List<CopyUsageInfo>> getCachedResul(String key) {
        SoftReference<CachedResult> value = cache.get(key);
        if (Objects.nonNull(value)) {
            CachedResult result = value.get();
            if (Objects.nonNull(result) && Objects.nonNull(result.getValue())) {
                return Optional.of(result.getValue());
            }
        }
        return Optional.empty();
    }


    /**
     * save find result in cache
     * @param key cache key
     * @param result find result
     * @param expireTime expire time in milliseconds
     */
    public void updateCacheResult(String key, List<CopyUsageInfo> result, Long expireTime) {
        cache.put(key, new SoftReference<>(new CachedResult(result, expireTime)));
    }

    /**
     * clear all cache
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * cache value
     */
    private static class CachedResult {
        private final List<CopyUsageInfo> value;
        private final long timestamp;
        private final long expireTime;

        CachedResult(List<CopyUsageInfo> value, Long expireTime) {
            this.value = Collections.unmodifiableList(value);
            this.timestamp = System.currentTimeMillis();
            this.expireTime = expireTime;
        }

        private boolean isExpired() {
            long elapsed = System.currentTimeMillis() - timestamp;
            return elapsed > expireTime;
        }

        public List<CopyUsageInfo> getValue() {
            return isExpired() ? null : value;
        }
    }

}