package vcat.cache;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.json.JSONObject;
import org.json.JSONTokener;

public class ApiCache extends StringFileCache {

	private final static String PREFIX = "ApiRequest-";

	private final static String SUFFIX = ".json";

	public ApiCache(File cacheDirectory) throws CacheException {
		super(cacheDirectory, PREFIX, SUFFIX);
	}

	public synchronized JSONObject getJSONObject(String key) throws CacheException {
		if (this.containsKey(key)) {
			InputStreamReader reader = new InputStreamReader(this.getAsInputStream(key));
			try {
				JSONObject result = new JSONObject(new JSONTokener(reader));
				return result;
			} catch (Exception e) {
				throw new CacheException("Error while parsing JSON data from cache", e);
			} finally {
				try {
					reader.close();
				} catch (IOException e) {
					throw new CacheException("Error closing reader after reading JSON data from cache", e);
				}
			}
		} else {
			return null;
		}
	}

	public synchronized void put(String key, JSONObject jsonObject) throws CacheException {
		this.put(key, jsonObject.toString().getBytes());
	}
}
