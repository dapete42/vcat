package org.toolforge.vcat.webapp.simple;

import jakarta.inject.Inject;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.toolforge.vcat.VCatException;
import org.toolforge.vcat.cache.interfaces.MetadataCache;
import org.toolforge.vcat.caffeine.ApiCaffeineCache;
import org.toolforge.vcat.caffeine.MetadataCaffeineCache;
import org.toolforge.vcat.graphviz.GraphvizExternal;
import org.toolforge.vcat.graphviz.QueuedGraphviz;
import org.toolforge.vcat.mediawiki.CachedApiClient;
import org.toolforge.vcat.mediawiki.CachedMetadataProvider;
import org.toolforge.vcat.mediawiki.interfaces.MetadataProvider;
import org.toolforge.vcat.params.AllParams;
import org.toolforge.vcat.renderer.CachedVCatRenderer;
import org.toolforge.vcat.renderer.QueuedVCatRenderer;
import org.toolforge.vcat.renderer.RenderedFileInfo;
import org.toolforge.vcat.renderer.interfaces.VCatRenderer;
import org.toolforge.vcat.webapp.base.AbstractVCatServlet;

import java.io.File;
import java.io.Serial;
import java.nio.file.Paths;

@WebServlet(urlPatterns = "/render")
public class SimpleVCatServlet extends AbstractVCatServlet {

    @Serial
    private static final long serialVersionUID = -6050180232379416509L;

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
     * Purge metadata after (seconds).
     */
    @Inject
    @ConfigProperty(name = "cache.purge.metadata", defaultValue = "86400")
    Integer cachePurgeMetadata;

    /**
     * Maximum number of concurrent threads running vCat (0=unlimited).
     */
    @Inject
    @ConfigProperty(name = "vcat.threads", defaultValue = "0")
    Integer vcatThreads;

    private static MetadataProvider metadataProvider;

    private static VCatRenderer vCatRenderer;

    @Override
    public String getServletInfo() {
        return "Simple vCat servlet";
    }

    @Override
    public void init() throws ServletException {
        final var cachePath = Paths.get(cacheDir);
        final var tempDir = ((File) this.getServletContext().getAttribute(ServletContext.TEMPDIR)).toPath();
        try {
            final var graphviz = new QueuedGraphviz(new GraphvizExternal(Paths.get(graphvizDir)), 1);
            final var apiCache = new ApiCaffeineCache(10000, cachePurge);
            final CachedApiClient apiClient = new CachedApiClient(apiCache);
            final MetadataCache metadataCache = new MetadataCaffeineCache(10000, cachePurgeMetadata);
            metadataProvider = new CachedMetadataProvider(apiClient, metadataCache);
            vCatRenderer = new QueuedVCatRenderer(
                    new CachedVCatRenderer(graphviz, tempDir, apiClient, cachePath, cachePurge), vcatThreads);
        } catch (VCatException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected RenderedFileInfo renderedFileFromRequest(final HttpServletRequest req) throws ServletException {
        try {
            return vCatRenderer.render(new AllParams(req.getParameterMap(), req.getRequestURI(), metadataProvider));
        } catch (VCatException e) {
            throw new ServletException(e);
        }
    }

}
