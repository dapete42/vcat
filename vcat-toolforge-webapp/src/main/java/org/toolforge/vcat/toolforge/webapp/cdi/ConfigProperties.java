package org.toolforge.vcat.toolforge.webapp.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Getter
@ApplicationScoped
public class ConfigProperties {

    /**
     * Maximum size for cache.
     */
    @Inject
    @ConfigProperty(name="cache.size", defaultValue = "10000")
    Integer cacheSize;

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

    /**
     * Directory with Graphviz binaries (dot, fdp).
     */
    @Inject
    @ConfigProperty(name = "graphviz.dir", defaultValue = "/usr/bin")
    String graphvizDir;

    /**
     * Maximum number of concurrent threads running graphviz (0=unlimited).
     */
    @Inject
    @ConfigProperty(name = "graphviz.threads", defaultValue = "0")
    Integer graphvizThreads;

    /**
     * Maximum number of concurrent threads running vCat (0=unlimited).
     */
    @Inject
    @ConfigProperty(name = "vcat.threads", defaultValue = "0")
    Integer vcatThreads;

}
