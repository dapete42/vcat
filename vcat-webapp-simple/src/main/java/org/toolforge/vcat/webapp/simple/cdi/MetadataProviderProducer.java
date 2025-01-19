package org.toolforge.vcat.webapp.simple.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.toolforge.vcat.caffeine.MetadataCaffeineCache;
import org.toolforge.vcat.mediawiki.ApiClient;
import org.toolforge.vcat.mediawiki.CachedMetadataProvider;
import org.toolforge.vcat.mediawiki.interfaces.MetadataProvider;
import org.toolforge.vcat.webapp.simple.cdi.qualifier.ApiClientQualifier;
import org.toolforge.vcat.webapp.simple.cdi.qualifier.MetadataProviderQualifier;

@ApplicationScoped
@Slf4j
public class MetadataProviderProducer {

    /**
     * Purge metadata after (seconds).
     */
    @Inject
    @ConfigProperty(name = "cache.purge.metadata", defaultValue = "86400")
    Integer cachePurgeMetadata;

    @Inject
    @ApiClientQualifier
    ApiClient apiClient;

    @Produces
    @MetadataProviderQualifier
    public MetadataProvider produceMetadataProvider() {
        final var metadataCache = new MetadataCaffeineCache(10000, cachePurgeMetadata);
        return new CachedMetadataProvider(apiClient, metadataCache);
    }

}
