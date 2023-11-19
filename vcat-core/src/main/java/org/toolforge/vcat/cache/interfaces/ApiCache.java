package org.toolforge.vcat.cache.interfaces;

import jakarta.json.JsonObject;
import org.toolforge.vcat.cache.CacheException;

public interface ApiCache {

    boolean containsKey(String key);

    JsonObject getJSONObject(String key) throws CacheException;

    void purge() throws CacheException;

    void put(String key, JsonObject jsonObject) throws CacheException;

}