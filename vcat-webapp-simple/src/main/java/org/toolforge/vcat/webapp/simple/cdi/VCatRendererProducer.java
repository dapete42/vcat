package org.toolforge.vcat.webapp.simple.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.toolforge.vcat.VCatException;
import org.toolforge.vcat.caffeine.ApiCaffeineCache;
import org.toolforge.vcat.graphviz.GraphvizExternal;
import org.toolforge.vcat.graphviz.QueuedGraphviz;
import org.toolforge.vcat.mediawiki.CachedApiClient;
import org.toolforge.vcat.renderer.CachedVCatRenderer;
import org.toolforge.vcat.renderer.QueuedVCatRenderer;
import org.toolforge.vcat.renderer.interfaces.VCatRenderer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@ApplicationScoped
@Slf4j
public class VCatRendererProducer {

    @Inject
    @ConfigProperty(name = "graphviz.dir", defaultValue = "/usr/bin")
    String graphvizDir;

    /**
     * Cache directory.
     */
    @Inject
    @ConfigProperty(name = "cache.dir", defaultValue = "/tmp/vcat")
    String cacheDir;

    /**
     * Purge caches after (seconds).
     */
    @Inject
    @ConfigProperty(name = "cache.purge", defaultValue = "600")
    Integer cachePurge;

    /**
     * Maximum number of concurrent threads running vCat (0=unlimited).
     */
    @Inject
    @ConfigProperty(name = "vcat.threads", defaultValue = "0")
    Integer vcatThreads;

    @Produces
    public VCatRenderer produceVCatRenderer() throws IOException, VCatException {
        final var cachePath = Paths.get(cacheDir);
        LOG.info("Using cache directory {}", cachePath);
        final var tempDir = Files.createTempDirectory("vcat-webapp-simple");
        LOG.info("Using temporary directory {}", tempDir);
        final var graphviz = new QueuedGraphviz(new GraphvizExternal(Paths.get(graphvizDir)), 1);
        final var apiCache = new ApiCaffeineCache(10000, cachePurge);
        final var apiClient = new CachedApiClient(apiCache);
        return new QueuedVCatRenderer(
                new CachedVCatRenderer(graphviz, tempDir, apiClient, cachePath, cachePurge), vcatThreads);
    }

}
