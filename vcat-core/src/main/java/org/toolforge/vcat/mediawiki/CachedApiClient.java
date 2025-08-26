package org.toolforge.vcat.mediawiki;

import jakarta.json.JsonObject;
import org.jspecify.annotations.Nullable;
import org.toolforge.vcat.Messages;
import org.toolforge.vcat.cache.CacheException;
import org.toolforge.vcat.cache.interfaces.ApiCache;

import java.io.Serial;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CachedApiClient extends ApiClient {

    @Serial
    private static final long serialVersionUID = -4286304620124061421L;

    private final ApiCache cache;

    private final Lock cacheLock = new ReentrantLock();

    public CachedApiClient(ApiCache cache) {
        this.cache = cache;
    }

    @Override
    @Nullable
    protected JsonObject request(String apiUrl, Map<String, String> params) throws ApiException {
        final StringBuilder requestStuff = new StringBuilder(apiUrl)
                .append('&');
        for (Entry<String, String> param : params.entrySet()) {
            requestStuff.append(URLEncoder.encode(param.getKey(), StandardCharsets.UTF_8))
                    .append('&')
                    .append(URLEncoder.encode(param.getValue(), StandardCharsets.UTF_8));
        }
        final String cacheKey = requestStuff.toString();

        cacheLock.lock();
        try {
            if (cache.containsKey(cacheKey)) {
                try {
                    return cache.getJSONObject(cacheKey);
                } catch (CacheException e) {
                    throw new ApiException(Messages.getString("CachedApiClient.Exception.AccessCache"), e);
                }
            } else {
                final var jsonObject = uncachedRequest(apiUrl, params);
                try {
                    cache.put(cacheKey, jsonObject);
                } catch (CacheException e) {
                    throw new ApiException(Messages.getString("CachedApiClient.Exception.CacheResult"), e);
                }
                return jsonObject;
            }
        } finally {
            cacheLock.unlock();
        }
    }

    protected JsonObject uncachedRequest(String apiUrl, Map<String, String> params) throws ApiException {
        return super.request(apiUrl, params);
    }

}
