package org.toolforge.vcat.webapp.simple.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.toolforge.vcat.webapp.simple.cdi.ConfigProperties;

@Liveness
@ApplicationScoped
public class ConfigurationCheck implements HealthCheck {

    @Inject
    ConfigProperties config;

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("Configuration")
                .up()
                .withData("Cache size", config.getCacheSize())
                .withData("Cache purge (seconds)", config.getCachePurge())
                .withData("Cache purge metadata (seconds)", config.getCachePurgeMetadata())
                .withData("VCat threads", config.getVcatThreads())
                .withData("Graphviz threads", config.getGraphvizThreads())
                .build();
    }

}
