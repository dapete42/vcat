package vcat.mediawiki;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;

import javax.json.JsonObject;

import vcat.Messages;
import vcat.cache.CacheException;
import vcat.cache.IApiCache;

public class CachedApiClient<W extends IWiki> extends ApiClient<W> {

	private final IApiCache cache;

	public CachedApiClient(IApiCache cache) {
		this.cache = cache;
	}

	public IApiCache getCache() {
		return cache;
	}

	@Override
	protected JsonObject request(String apiUrl, Map<String, String> params) throws ApiException {
		StringBuilder requestStuff = new StringBuilder();
		requestStuff.append(apiUrl);
		requestStuff.append('&');
		for (Entry<String, String> param : params.entrySet()) {
			try {
				requestStuff.append(URLEncoder.encode(param.getKey(), StandardCharsets.UTF_8.name()));
				requestStuff.append('&');
				requestStuff.append(URLEncoder.encode(param.getValue(), StandardCharsets.UTF_8.name()));
			} catch (UnsupportedEncodingException e) {
				throw new ApiException(Messages.getString("Exception.UTF8"), e);
			}
		}
		String cacheKey = requestStuff.toString();

		synchronized (cache) {
			if (this.cache.containsKey(cacheKey)) {
				try {
					return this.cache.getJSONObject(cacheKey);
				} catch (CacheException e) {
					throw new ApiException(Messages.getString("CachedApiClient.Exception.AccessCache"), e);
				}
			} else {
				JsonObject jsonObject = super.request(apiUrl, params);
				try {
					this.cache.put(cacheKey, jsonObject);
				} catch (CacheException e) {
					throw new ApiException(Messages.getString("CachedApiClient.Exception.CacheResult"), e);
				}
				return jsonObject;
			}
		}
	}

}
