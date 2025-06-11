package org.toolforge.vcat.toolforge.webapp.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.toolforge.vcat.cache.CacheException;
import org.toolforge.vcat.graphviz.QueuedGraphviz;
import org.toolforge.vcat.graphviz.interfaces.Graphviz;
import org.toolforge.vcat.mediawiki.ApiClient;
import org.toolforge.vcat.mediawiki.CachedApiClient;
import org.toolforge.vcat.mediawiki.CachedMetadataProvider;
import org.toolforge.vcat.mediawiki.interfaces.MetadataProvider;
import org.toolforge.vcat.toolforge.webapp.cdi.qualifier.ApiClientQualifier;
import org.toolforge.vcat.toolforge.webapp.cdi.qualifier.MetadataProviderQualifier;

@Liveness
@ApplicationScoped
public class StatusCheck implements HealthCheck {

    @Inject
    @ApiClientQualifier
    ApiClient apiClient;

    @Inject
    Graphviz graphviz;

    @Inject
    @MetadataProviderQualifier
    MetadataProvider metadataProvider;

    @Override
    public HealthCheckResponse call() {
        final var builder = HealthCheckResponse.named("Status")
                .up();
        if (apiClient instanceof CachedApiClient cachedApiClient) {
            try {
                builder.withData("API client cache size", cachedApiClient.currentCacheSize());
            } catch (CacheException e) {
                builder.withData("API client cache size", "unknown");
            }
        }
        if (metadataProvider instanceof CachedMetadataProvider cachedMetadataProvider) {
            try {
                builder.withData("Metadata cache size", cachedMetadataProvider.currentCacheSize());
            } catch (CacheException e) {
                builder.withData("Metadata cache size", "unknown");
            }
        }
        if (graphviz instanceof QueuedGraphviz queuedGraphviz) {
            builder.withData("Graphviz queue length", queuedGraphviz.getQueueLength());
        }
        return builder.build();
    }

}
