package vcat.cache;

import javax.json.JsonObject;

public interface IApiCache {

	boolean containsKey(String key);

	JsonObject getJSONObject(String key) throws CacheException;

	void purge();

	void put(String key, JsonObject jsonObject) throws CacheException;

}