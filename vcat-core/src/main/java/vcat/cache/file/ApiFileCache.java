package vcat.cache.file;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;

import vcat.Messages;
import vcat.cache.CacheException;
import vcat.cache.IApiCache;

public class ApiFileCache extends StringFileCache implements IApiCache {

	private final static String PREFIX = "ApiRequest-";

	private final static String SUFFIX = ".json";

	public ApiFileCache(final File cacheDirectory, final int maxAgeInSeconds) throws CacheException {
		super(cacheDirectory, PREFIX, SUFFIX, maxAgeInSeconds);
	}

	@Override
	public synchronized JsonObject getJSONObject(String key) throws CacheException {
		if (this.containsKey(key)) {
			try (InputStreamReader reader = new InputStreamReader(this.getAsInputStream(key))) {
				JsonObject result = Json.createReader(reader).readObject();
				return result;
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
