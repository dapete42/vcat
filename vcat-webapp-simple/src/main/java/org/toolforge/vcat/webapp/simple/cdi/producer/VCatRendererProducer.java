package org.toolforge.vcat.webapp.simple.cdi.producer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.toolforge.vcat.VCatException;
import org.toolforge.vcat.graphviz.interfaces.Graphviz;
import org.toolforge.vcat.mediawiki.ApiClient;
import org.toolforge.vcat.renderer.CachedVCatRenderer;
import org.toolforge.vcat.renderer.QueuedVCatRenderer;
import org.toolforge.vcat.renderer.interfaces.VCatRenderer;
import org.toolforge.vcat.webapp.simple.cdi.ConfigProperties;
import org.toolforge.vcat.webapp.simple.cdi.qualifier.ApiClientQualifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@ApplicationScoped
@Slf4j
public class VCatRendererProducer {

    @Inject
    ConfigProperties config;

    @Produces
    public VCatRenderer produceVCatRenderer(@ApiClientQualifier ApiClient apiClient, Graphviz graphviz) throws IOException, VCatException {
        final var cachePath = Paths.get(config.getCacheDir());
        LOG.info("Using cache directory {}", cachePath);
        final var tempDir = Files.createTempDirectory("vcat-webapp-simple");
        LOG.info("Using temporary directory {}", tempDir);
        return new QueuedVCatRenderer(
                new CachedVCatRenderer(graphviz, tempDir, apiClient, cachePath, config.getCachePurge()),
                config.getVcatThreads());
    }

}
