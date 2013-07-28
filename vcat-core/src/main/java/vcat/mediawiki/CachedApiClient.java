package vcat.mediawiki;

import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONObject;

import vcat.cache.ApiCache;
import vcat.cache.CacheException;


public class CachedApiClient extends ApiClient {

	private final ApiCache cache;

	public CachedApiClient(IWiki wiki, ApiCache cache) {
		super(wiki);
		this.cache = cache;
	}

	public ApiCache getCache() {
		return cache;
	}

	@Override
	protected JSONObject request(Map<String, String> params) throws ApiException {
		StringBuilder requestStuff = new StringBuilder();
		requestStuff.append(this.wiki.getApiUrl());
		requestStuff.append('&');
		for (Entry<String, String> param : params.entrySet()) {
			requestStuff.append(param.getKey());
			requestStuff.append('&');
			requestStuff.append(param.getValue());
		}
		String cacheKey = requestStuff.toString();
		if (this.cache.containsKey(cacheKey)) {
			try {
				return this.cache.getJSONObject(cacheKey);
			} catch (CacheException e) {
				throw new ApiException("Error accessing API cache", e);
			}
		} else {
			JSONObject jsonObject = super.request(params);
			try {
				this.cache.put(cacheKey, jsonObject);
			} catch (CacheException e) {
				throw new ApiException("Error caching API result", e);
			}
			return jsonObject;
		}
	}

}
