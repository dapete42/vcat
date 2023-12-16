package org.toolforge.vcat.webapp.simple.rest;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
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
import org.toolforge.vcat.renderer.interfaces.VCatRenderer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.StringJoiner;
import java.util.stream.Stream;

@Slf4j
@Path("/render")
public class SimpleVCatResource {

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

    private static Response errorResponse(Response.Status status, String message) {
        return Response.status(status)
                .entity(status.getReasonPhrase() + '\n' + message)
                .type(MediaType.TEXT_PLAIN_TYPE)
                .build();
    }

    private static String exceptionStringWithStacktrace(Exception e) {
        final var joiner = new StringJoiner("\n");
        joiner.add(e.getMessage());
        joiner.add("");
        Stream.of(e.getStackTrace())
                .forEach(stackTraceElement -> joiner.add(stackTraceElement.toString()));
        return joiner.toString();
    }

    private static String uriStringWithoutQuery(UriInfo uriInfo) {
        return uriInfo.getRequestUriBuilder().replaceQuery(null).build().toString();
    }

    @PostConstruct
    public void init() throws IOException, VCatException {
        final var cachePath = Paths.get(cacheDir);
        LOG.info("Using cache directory {}", cachePath);
        final var tempDir = Files.createTempDirectory("vcat-webapp-simple");
        LOG.info("Using temporary directory {}", tempDir);
        final var graphviz = new QueuedGraphviz(new GraphvizExternal(Paths.get(graphvizDir)), 1);
        final var apiCache = new ApiCaffeineCache(10000, cachePurge);
        final CachedApiClient apiClient = new CachedApiClient(apiCache);
        final MetadataCache metadataCache = new MetadataCaffeineCache(10000, cachePurgeMetadata);
        metadataProvider = new CachedMetadataProvider(apiClient, metadataCache);
        vCatRenderer = new QueuedVCatRenderer(
                new CachedVCatRenderer(graphviz, tempDir, apiClient, cachePath, cachePurge), vcatThreads);
    }

    @GET
    public Response render(@Context UriInfo uriInfo) {
        try {
            final var renderedFileInfo = vCatRenderer.render(
                    new AllParams(uriInfo.getQueryParameters(), uriStringWithoutQuery(uriInfo), metadataProvider));

            final var resultFile = renderedFileInfo.getFile();
            final var mimeType = renderedFileInfo.getMimeType();

            LOG.info("Sending file '{}' as '{}'", resultFile.toAbsolutePath(), mimeType);
            return Response.ok(Files.newInputStream(resultFile))
                    // Content-disposition (for file name)
                    .header("Content-disposition", "filename=\"" + resultFile.getFileName().toString() + '"')
                    .type(mimeType)
                    .build();
        } catch (VCatException e) {
            return errorResponse(Response.Status.BAD_REQUEST, exceptionStringWithStacktrace(e));
        } catch (IOException e) {
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

}
