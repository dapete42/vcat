package vcat.mediawiki;

import jakarta.json.JsonObject;
import lombok.Getter;
import vcat.Messages;
import vcat.cache.CacheException;
import vcat.cache.interfaces.ApiCache;
import vcat.mediawiki.interfaces.Wiki;

import java.io.Serial;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;

@Getter
public class CachedApiClient<W extends Wiki> extends ApiClient<W> {

    @Serial
    private static final long serialVersionUID = -4286304620124061421L;

    private final ApiCache cache;

    public CachedApiClient(ApiCache cache) {
        this.cache = cache;
    }

    @Override
    protected JsonObject request(String apiUrl, Map<String, String> params) throws ApiException {
        final StringBuilder requestStuff = new StringBuilder(apiUrl)
                .append('&');
        for (Entry<String, String> param : params.entrySet()) {
            requestStuff.append(URLEncoder.encode(param.getKey(), StandardCharsets.UTF_8))
                    .append('&')
                    .append(URLEncoder.encode(param.getValue(), StandardCharsets.UTF_8));
        }
        final String cacheKey = requestStuff.toString();

        synchronized (cache) {
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
        }
    }

    protected JsonObject uncachedRequest(String apiUrl, Map<String, String> params) throws ApiException {
        return super.request(apiUrl, params);
    }

}
