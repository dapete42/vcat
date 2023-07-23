package vcat.cache;

import java.io.Serializable;

import jakarta.json.JsonObject;

public interface IApiCache extends Serializable {

	boolean containsKey(String key);

	JsonObject getJSONObject(String key) throws CacheException;

	void purge() throws CacheException;

	void put(String key, JsonObject jsonObject) throws CacheException;

}