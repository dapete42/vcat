package org.toolforge.vcat.caffeine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.json.JsonObject;
import org.jspecify.annotations.Nullable;
import org.toolforge.vcat.cache.interfaces.ApiCache;

import java.time.Duration;

public class ApiCaffeineCache implements ApiCache {

    private final Cache<String, JsonObject> cache;

    public ApiCaffeineCache(int size, int timeout) {
        cache = Caffeine.newBuilder()
                .maximumSize(size)
                .expireAfterWrite(Duration.ofSeconds(timeout))
                .build();
    }

    @Override
    public boolean containsKey(String key) {
        return cache.getIfPresent(key) != null;
    }

    @Override
    @Nullable
    public JsonObject getJSONObject(String key) {
        return cache.getIfPresent(key);
    }

    @Override
    public void purge() {
        cache.cleanUp();
    }

    @Override
    public void put(String key, JsonObject jsonObject) {
        cache.put(key, jsonObject);
    }

}
