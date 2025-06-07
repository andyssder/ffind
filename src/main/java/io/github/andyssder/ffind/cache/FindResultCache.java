package io.github.andyssder.ffind.cache;

import io.github.andyssder.ffind.model.idea.CopyUsageInfo;
import com.intellij.openapi.application.ReadAction;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import java.lang.ref.SoftReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

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
     * @param field the field which user wants to find
     * @return cached result if exist or empty.
     */
    public Optional<List<CopyUsageInfo>> getCachedResultByField(PsiField field) {
        String key = generateKeyForField(field);
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
     * @param field the field which user wants to find
     * @param result find result
     * @param expireTime expire time in milliseconds
     */
    public void updateCache(PsiField field, List<CopyUsageInfo> result, Long expireTime) {
        String key = generateKeyForField(field);
        cache.put(key, new SoftReference<>(new CachedResult(result, expireTime)));
    }

    /**
     * clear all cache
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * generate cache key for field: className#fieldName
     * @param field the field which user wants to find
     * @return cache key
     */
    private String generateKeyForField(PsiField field) {
        AtomicReference<String> key = new AtomicReference<>();
        ReadAction.compute(() -> {
            PsiClass containingClass = field.getContainingClass();
            String className = containingClass != null ? containingClass.getQualifiedName() : "Anonymous";
            key.set(className + "#" + field.getName());
            return true;
        });
        return key.get();
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