package vcat.webapp.simple;

import jakarta.inject.Inject;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import vcat.VCatException;
import vcat.cache.interfaces.MetadataCache;
import vcat.caffeine.ApiCaffeineCache;
import vcat.caffeine.MetadataCaffeineCache;
import vcat.graphviz.GraphvizExternal;
import vcat.graphviz.QueuedGraphviz;
import vcat.mediawiki.CachedApiClient;
import vcat.mediawiki.CachedMetadataProvider;
import vcat.mediawiki.SimpleWikimediaWiki;
import vcat.mediawiki.interfaces.MetadataProvider;
import vcat.params.AllParams;
import vcat.renderer.CachedVCatRenderer;
import vcat.renderer.interfaces.VCatRenderer;
import vcat.renderer.QueuedVCatRenderer;
import vcat.renderer.RenderedFileInfo;
import vcat.webapp.base.AbstractVCatServlet;

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

    private static VCatRenderer<SimpleWikimediaWiki> vCatRenderer;

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
            final CachedApiClient<SimpleWikimediaWiki> apiClient = new CachedApiClient<>(apiCache);
            final MetadataCache metadataCache = new MetadataCaffeineCache(10000, cachePurgeMetadata);
            metadataProvider = new CachedMetadataProvider(apiClient, metadataCache);
            vCatRenderer = new QueuedVCatRenderer<>(
                    new CachedVCatRenderer<>(graphviz, tempDir, apiClient, cachePath, cachePurge), vcatThreads);
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
