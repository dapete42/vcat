package org.toolforge.vcat.webapp.simple.cdi.producer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.toolforge.vcat.cache.interfaces.ApiCache;
import org.toolforge.vcat.mediawiki.ApiClient;
import org.toolforge.vcat.mediawiki.CachedApiClient;
import org.toolforge.vcat.webapp.simple.cdi.qualifier.ApiClientQualifier;

@ApplicationScoped
public class ApiClientProducer {

    @Produces
    @ApiClientQualifier
    ApiClient produceApiClient(ApiCache apiCache) {
        return new CachedApiClient(apiCache);
    }

}
