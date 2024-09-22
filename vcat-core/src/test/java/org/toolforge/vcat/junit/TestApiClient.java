package org.toolforge.vcat.junit;

import jakarta.json.JsonObject;
import lombok.Getter;
import lombok.Setter;
import org.toolforge.vcat.cache.CacheException;
import org.toolforge.vcat.cache.file.ApiFileCache;
import org.toolforge.vcat.mediawiki.ApiException;
import org.toolforge.vcat.mediawiki.CachedApiClient;

import java.io.Serial;
import java.util.Map;

public class TestApiClient extends CachedApiClient {

    @Serial
    private static final long serialVersionUID = 1969628748792074157L;

    @Getter
    @Setter
    private boolean callRealApi = false;

    public TestApiClient() throws CacheException {
        super(new ApiFileCache(TestUtils.testApiClientCacheDirectory, -1));
    }

    @Override
    protected JsonObject uncachedRequest(String apiUrl, Map<String, String> params) throws ApiException {
        if (callRealApi) {
            return super.uncachedRequest(apiUrl, params);
        } else {
            throw new ApiException("TestApiClient will not send real HTTP requests");
        }
    }

}
