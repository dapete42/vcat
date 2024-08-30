package org.toolforge.vcat.toolforge.webapp.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.toolforge.vcat.caffeine.ApiCaffeineCache;
import org.toolforge.vcat.mediawiki.ApiClient;
import org.toolforge.vcat.mediawiki.CachedApiClient;
import org.toolforge.vcat.toolforge.webapp.cdi.qualifier.ApiClientQualifier;

@ApplicationScoped
public class ApiClientProducer {

    /**
     * Purge caches after (seconds).
     */
    @Inject
    @ConfigProperty(name = "cache.purge", defaultValue = "600")
    Integer cachePurge;

    @Produces
    @ApiClientQualifier
    ApiClient produceApiClient() {
        // Use Caffeine for API and metadata caches
        final var apiCache = new ApiCaffeineCache(10000, cachePurge);
        return new CachedApiClient(apiCache);
    }

}
