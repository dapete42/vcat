package vcat.cache;

import org.json.JSONObject;

public interface IApiCache {

	boolean containsKey(String key);

	JSONObject getJSONObject(String key) throws CacheException;

	void purge();

	void put(String key, JSONObject jsonObject) throws CacheException;

}