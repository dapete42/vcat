package vcat.cache.file;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.json.JSONObject;
import org.json.JSONTokener;

import vcat.cache.CacheException;
import vcat.cache.IApiCache;

public class ApiFileCache extends StringFileCache implements IApiCache {

	private final static String PREFIX = "ApiRequest-";

	private final static String SUFFIX = ".json";

	public ApiFileCache(final File cacheDirectory, final int maxAgeInSeconds) throws CacheException {
		super(cacheDirectory, PREFIX, SUFFIX, maxAgeInSeconds);
	}

	@Override
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

	@Override
	public synchronized void put(String key, JSONObject jsonObject) throws CacheException {
		this.put(key, jsonObject.toString().getBytes());
	}

}
