package org.toolforge.vcat.toolforge.webapp.cdi.producer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.toolforge.vcat.VCatException;
import org.toolforge.vcat.graphviz.interfaces.Graphviz;
import org.toolforge.vcat.mediawiki.ApiClient;
import org.toolforge.vcat.renderer.CachedVCatRenderer;
import org.toolforge.vcat.renderer.QueuedVCatRenderer;
import org.toolforge.vcat.renderer.interfaces.VCatRenderer;
import org.toolforge.vcat.toolforge.webapp.cdi.ConfigProperties;
import org.toolforge.vcat.toolforge.webapp.cdi.qualifier.ApiClientQualifier;

import java.io.IOException;
import java.nio.file.Files;

@ApplicationScoped
public class VCatRendererProducer {

    @Inject
    ConfigProperties config;

    @Produces
    public VCatRenderer produceVCatRenderer(@ApiClientQualifier ApiClient apiClient, Graphviz graphviz) throws IOException, VCatException {
        // For cache of Graphviz files and rendered images, use this directory
        final var cacheDir = Files.createTempDirectory("vcat-cache");
        // Temporary directory for Graphviz files and rendered images
        final var tempDir = Files.createTempDirectory("vcat-temp");

        Files.createDirectories(cacheDir);
        Files.createDirectories(tempDir);

        return new QueuedVCatRenderer(
                new CachedVCatRenderer(graphviz, tempDir, apiClient, cacheDir, config.getCachePurge()),
                config.getVcatThreads());
    }

}
