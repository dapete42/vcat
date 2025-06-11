package org.toolforge.vcat.webapp.simple.cdi.producer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.toolforge.vcat.cache.interfaces.ApiCache;
import org.toolforge.vcat.caffeine.ApiCaffeineCache;
import org.toolforge.vcat.webapp.simple.cdi.ConfigProperties;

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
