package org.toolforge.vcat.cache.file;

import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.toolforge.vcat.Messages;
import org.toolforge.vcat.cache.CacheException;
import org.toolforge.vcat.cache.interfaces.ApiCache;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;

public class ApiFileCache extends AbstractFileCache<String> implements ApiCache {

    private static final String PREFIX = "ApiRequest-";

    private static final String SUFFIX = ".json";

    public ApiFileCache(final Path cacheDirectory, final int maxAgeInSeconds) throws CacheException {
        super(cacheDirectory, PREFIX, SUFFIX, maxAgeInSeconds);
    }

    @Override
    public synchronized JsonObject getJSONObject(String key) throws CacheException {
        if (this.containsKey(key)) {
            try (InputStreamReader reader = new InputStreamReader(this.getAsInputStream(key));
                 JsonReader jsonReader = Json.createReader(reader)) {
                return jsonReader.readObject();
            } catch (JsonException e) {
                throw new CacheException(Messages.getString("ApiFileCache.Exception.ParseJSON"), e);
            } catch (IOException e) {
                throw new CacheException(Messages.getString("ApiFileCache.Exception.CloseJSON"), e);
            }
        } else {
            return null;
        }
    }

    @Override
    public synchronized void put(String key, JsonObject jsonObject) throws CacheException {
        this.put(key, jsonObject.toString().getBytes());
    }

}
