package org.toolforge.vcat.webapp.simple.cdi.producer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.toolforge.vcat.caffeine.MetadataCaffeineCache;
import org.toolforge.vcat.mediawiki.ApiClient;
import org.toolforge.vcat.mediawiki.CachedMetadataProvider;
import org.toolforge.vcat.mediawiki.interfaces.MetadataProvider;
import org.toolforge.vcat.webapp.simple.cdi.ConfigProperties;
import org.toolforge.vcat.webapp.simple.cdi.qualifier.ApiClientQualifier;
import org.toolforge.vcat.webapp.simple.cdi.qualifier.MetadataProviderQualifier;

@ApplicationScoped
@Slf4j
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
