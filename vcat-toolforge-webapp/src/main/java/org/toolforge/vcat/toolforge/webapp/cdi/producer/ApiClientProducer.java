package org.toolforge.vcat.toolforge.webapp.cdi.producer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.toolforge.vcat.cache.interfaces.ApiCache;
import org.toolforge.vcat.mediawiki.ApiClient;
import org.toolforge.vcat.mediawiki.CachedApiClient;
import org.toolforge.vcat.toolforge.webapp.cdi.ConfigProperties;
import org.toolforge.vcat.toolforge.webapp.cdi.qualifier.ApiClientQualifier;

@ApplicationScoped
public class ApiClientProducer {

    @Inject
    ConfigProperties config;

    @Produces
    @ApiClientQualifier
    ApiClient produceApiClient(ApiCache apiCache) {
        return new CachedApiClient(apiCache);
    }

}
