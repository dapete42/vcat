package org.toolforge.vcat.toolforge.webapp.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.toolforge.vcat.VCatException;
import org.toolforge.vcat.graphviz.GraphvizExternal;
import org.toolforge.vcat.graphviz.QueuedGraphviz;
import org.toolforge.vcat.mediawiki.ApiClient;
import org.toolforge.vcat.renderer.CachedVCatRenderer;
import org.toolforge.vcat.renderer.QueuedVCatRenderer;
import org.toolforge.vcat.renderer.interfaces.VCatRenderer;
import org.toolforge.vcat.toolforge.webapp.cdi.qualifier.ApiClientQualifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@ApplicationScoped
public class VCatRendererProducer {

    /**
     * Directory with Graphviz binaries (dot, fdp).
     */
    @Inject
    @ConfigProperty(name = "graphviz.dir", defaultValue = "/usr/bin")
    String graphvizDir;

    /**
     * Purge caches after (seconds).
     */
    @Inject
    @ConfigProperty(name = "cache.purge", defaultValue = "600")
    Integer cachePurge;

    /**
     * Maxim number of concurrent threads running graphviz (0=unlimited).
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

    @Inject
    @ApiClientQualifier
    ApiClient apiClient;

    @Produces
    public VCatRenderer VCatRenderer() throws IOException, VCatException {
        // For cache of Graphviz files and rendered images, use this directory
        final var cacheDir = Files.createTempDirectory("vcat-cache");
        // Temporary directory for Graphviz files and rendered images
        final var tempDir = Files.createTempDirectory("vcat-temp");

        Files.createDirectories(cacheDir);
        Files.createDirectories(tempDir);

        final var graphvizDirPath = Paths.get(graphvizDir);
        final var baseGraphviz = new GraphvizExternal(graphvizDirPath);
        final var graphviz = new QueuedGraphviz(baseGraphviz, graphvizThreads);

        return new QueuedVCatRenderer(
                new CachedVCatRenderer(graphviz, tempDir, apiClient, cacheDir, cachePurge),
                vcatThreads);
    }

}
