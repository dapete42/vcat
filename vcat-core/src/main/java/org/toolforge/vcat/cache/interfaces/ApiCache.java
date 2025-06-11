package org.toolforge.vcat.cache.interfaces;

import jakarta.json.JsonObject;
import org.jspecify.annotations.Nullable;
import org.toolforge.vcat.cache.CacheException;

public interface ApiCache {

    boolean containsKey(String key);

    @Nullable
    JsonObject getJSONObject(String key) throws CacheException;

    void purge() throws CacheException;

    void put(String key, JsonObject jsonObject) throws CacheException;

    long currentSize() throws CacheException;

}
