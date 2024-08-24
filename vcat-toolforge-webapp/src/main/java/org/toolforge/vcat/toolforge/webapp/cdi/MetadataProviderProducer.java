package org.toolforge.vcat.toolforge.webapp.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.toolforge.vcat.caffeine.MetadataCaffeineCache;
import org.toolforge.vcat.mediawiki.ApiClient;
import org.toolforge.vcat.mediawiki.CachedMetadataProvider;
import org.toolforge.vcat.mediawiki.interfaces.MetadataProvider;
import org.toolforge.vcat.toolforge.webapp.cdi.qualifier.ApiClientQualifier;
import org.toolforge.vcat.toolforge.webapp.cdi.qualifier.MetadataProviderQualifier;

@ApplicationScoped
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
