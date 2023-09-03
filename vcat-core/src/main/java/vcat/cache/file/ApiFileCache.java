package vcat.cache.file;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;

import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import vcat.Messages;
import vcat.cache.CacheException;
import vcat.cache.IApiCache;

public class ApiFileCache extends AbstractFileCache<String> implements IApiCache {

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
