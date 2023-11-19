package org.toolforge.vcat.webapp.simple;

import jakarta.inject.Inject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@WebServlet(urlPatterns = "/render")
@Slf4j
public class SimpleVCatServlet extends HttpServlet {

    @Serial
    private static final long serialVersionUID = 7822892157349895078L;

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

    private MetadataProvider metadataProvider;

    private VCatRenderer vCatRenderer;

    @Override
    public String getServletInfo() {
        return "Simple vCat servlet";
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        final var cachePath = Paths.get(cacheDir);
        LOG.info("Using cache directory {}", cachePath);
        final var tempdirServletContextAttribute = getServletContext().getAttribute(ServletContext.TEMPDIR);
        try {
            final Path tempDir;
            if (tempdirServletContextAttribute instanceof File tempDirFile) {
                tempDir = tempDirFile.toPath();
            } else {
                tempDir = Files.createTempDirectory("vcat-webapp-simple");
            }
            LOG.info("Using temporary directory {}", tempDir);
            final var graphviz = new QueuedGraphviz(new GraphvizExternal(Paths.get(graphvizDir)), 1);
            final var apiCache = new ApiCaffeineCache(10000, cachePurge);
            final CachedApiClient apiClient = new CachedApiClient(apiCache);
            final MetadataCache metadataCache = new MetadataCaffeineCache(10000, cachePurgeMetadata);
            metadataProvider = new CachedMetadataProvider(apiClient, metadataCache);
            vCatRenderer = new QueuedVCatRenderer(
                    new CachedVCatRenderer(graphviz, tempDir, apiClient, cachePath, cachePurge), vcatThreads);
        } catch (IOException | VCatException e) {
            throw new ServletException(e);
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
        this.doRequest(req, resp);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
        this.doRequest(req, resp);
    }

    protected void doRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
        try {
            RenderedFileInfo renderedFileInfo = vCatRenderer.render(
                    new AllParams(req.getParameterMap(), req.getRequestURI(), metadataProvider));

            // Get finished rendered file
            Path resultFile = renderedFileInfo.getFile();

            // Content-type
            String contentType = renderedFileInfo.getMimeType();
            resp.setContentType(contentType);

            // Content-length
            long length = Files.size(resultFile);
            if (length < Integer.MAX_VALUE) {
                resp.setContentLength((int) length);
            }

            // Content-disposition (for file name)
            String filename = resultFile.getFileName().toString();
            resp.setHeader("Content-disposition", "filename=\"" + filename + '"');

            // Serve file to browser
            try (InputStream renderedInput = Files.newInputStream(resultFile);
                 ServletOutputStream output = resp.getOutputStream()) {
                renderedInput.transferTo(output);
            }

            LOG.info("File sent: '{}' sent as '{}', {} bytes", resultFile.toAbsolutePath(), contentType, length);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

}
