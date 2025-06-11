package org.toolforge.vcat.toolforge.webapp.cdi.producer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.toolforge.vcat.cache.interfaces.ApiCache;
import org.toolforge.vcat.caffeine.ApiCaffeineCache;
import org.toolforge.vcat.toolforge.webapp.cdi.ConfigProperties;

@ApplicationScoped
public class ApiCacheProducer {

    @Inject
    ConfigProperties config;

    @Produces
    ApiCache produceApiCache() {
        // Use Caffeine for API and metadata caches
        return new ApiCaffeineCache(config.getCacheSize(), config.getCachePurge());
    }

}
