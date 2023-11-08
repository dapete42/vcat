package vcat.cache.interfaces;

import jakarta.json.JsonObject;
import vcat.cache.CacheException;

public interface ApiCache {

    boolean containsKey(String key);

    JsonObject getJSONObject(String key) throws CacheException;

    void purge() throws CacheException;

    void put(String key, JsonObject jsonObject) throws CacheException;

}