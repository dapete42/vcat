package org.toolforge.vcat.webapp.simple.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.toolforge.vcat.caffeine.ApiCaffeineCache;
import org.toolforge.vcat.caffeine.MetadataCaffeineCache;
import org.toolforge.vcat.mediawiki.CachedApiClient;
import org.toolforge.vcat.mediawiki.CachedMetadataProvider;
import org.toolforge.vcat.mediawiki.interfaces.MetadataProvider;

@ApplicationScoped
@Slf4j
public class MetadataProviderProducer {

    /**
     * Purge caches after (seconds).
     */
    @Inject
    @ConfigProperty(name = "cache.purge", defaultValue = "600")
    Integer cachePurge;

    /**
     * Purge metadata after (seconds).
     */
    @Inject
    @ConfigProperty(name = "cache.purge.metadata", defaultValue = "86400")
    Integer cachePurgeMetadata;

    @Produces
    public MetadataProvider produceMetadataProvider() {
        final var apiCache = new ApiCaffeineCache(10000, cachePurge);
        final var apiClient = new CachedApiClient(apiCache);
        final var metadataCache = new MetadataCaffeineCache(10000, cachePurgeMetadata);
        return new CachedMetadataProvider(apiClient, metadataCache);
    }

}
