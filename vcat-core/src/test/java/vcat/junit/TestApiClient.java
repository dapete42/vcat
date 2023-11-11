package vcat.junit;

import jakarta.json.JsonObject;
import lombok.Getter;
import lombok.Setter;
import vcat.cache.CacheException;
import vcat.cache.file.ApiFileCache;
import vcat.mediawiki.ApiException;
import vcat.mediawiki.CachedApiClient;

import java.io.Serial;
import java.nio.file.Path;
import java.util.Map;

public class TestApiClient extends CachedApiClient {

    @Serial
    private static final long serialVersionUID = 4814974369474303893L;

    @Getter
    @Setter
    private boolean callRealApi = false;

    public TestApiClient() throws CacheException {
        this(TestUtils.testApiClientCacheDirectory);
    }

    TestApiClient(Path cachePath) throws CacheException {
        super(new ApiFileCache(cachePath, -1));
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
