package org.toolforge.vcat.toolforge.webapp.cdi.producer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.toolforge.vcat.caffeine.MetadataCaffeineCache;
import org.toolforge.vcat.mediawiki.ApiClient;
import org.toolforge.vcat.mediawiki.CachedMetadataProvider;
import org.toolforge.vcat.mediawiki.interfaces.MetadataProvider;
import org.toolforge.vcat.toolforge.webapp.cdi.ConfigProperties;
import org.toolforge.vcat.toolforge.webapp.cdi.qualifier.ApiClientQualifier;
import org.toolforge.vcat.toolforge.webapp.cdi.qualifier.MetadataProviderQualifier;

@ApplicationScoped
public class MetadataProviderProducer {

    @Inject
    ConfigProperties config;

    @Produces
    @MetadataProviderQualifier
    public MetadataProvider produceMetadataProvider(@ApiClientQualifier ApiClient apiClient) {
        final var metadataCache = new MetadataCaffeineCache(config.getCacheSize(), config.getCachePurgeMetadata());
        return new CachedMetadataProvider(apiClient, metadataCache);
    }

}
