package org.toolforge.vcat.toolforge.webapp.rest;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.*;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.toolforge.vcat.VCatException;
import org.toolforge.vcat.caffeine.ApiCaffeineCache;
import org.toolforge.vcat.caffeine.MetadataCaffeineCache;
import org.toolforge.vcat.graphviz.GraphvizExternal;
import org.toolforge.vcat.graphviz.QueuedGraphviz;
import org.toolforge.vcat.mediawiki.CachedApiClient;
import org.toolforge.vcat.mediawiki.CachedMetadataProvider;
import org.toolforge.vcat.mediawiki.interfaces.MetadataProvider;
import org.toolforge.vcat.renderer.CachedVCatRenderer;
import org.toolforge.vcat.renderer.QueuedVCatRenderer;
import org.toolforge.vcat.toolforge.webapp.AllParamsToolforge;
import org.toolforge.vcat.toolforge.webapp.Messages;
import org.toolforge.vcat.toolforge.webapp.MyCnfConfig;
import org.toolforge.vcat.toolforge.webapp.ToolforgeWikiProvider;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Path("/render")
public class ToolforgeVCatResource {

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
     * Purge metadata after (seconds).
     */
    @Inject
    @ConfigProperty(name = "cache.purge.metadata", defaultValue = "86400")
    Integer cachePurgeMetadata;

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

    /**
     * Maximum number of processes to queue vor vCat
     */
    @Inject
    @ConfigProperty(name = "vcat.queue", defaultValue = "100")
    Integer vcatQueue;

    /**
     * Injected Quarkus data source
     */
    @Inject
    DataSource dataSource;

    private static QueuedVCatRenderer vCatRenderer;

    private static MetadataProvider metadataProvider;

    private static ToolforgeWikiProvider toolforgeWikiProvider;

    private static Map<String, String[]> convertRestParameters(
            MultivaluedMap<String, String> parameters) {
        return parameters.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().toArray(String[]::new)
                ));
    }

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
    public void init() throws ServletException {

        try {

            final var configMyCnf = new MyCnfConfig();
            configMyCnf.readFromMyCnf();

            // Provider for Wikimedia Toolforge wiki information
            toolforgeWikiProvider = new ToolforgeWikiProvider(dataSource);

            // Use Caffeine for API and metadata caches
            final var apiCache = new ApiCaffeineCache(10000, cachePurge);
            final var metadataCache = new MetadataCaffeineCache(10000, cachePurgeMetadata);

            final CachedApiClient apiClient = new CachedApiClient(apiCache);

            // Metadata provider
            metadataProvider = new CachedMetadataProvider(apiClient, metadataCache);

            // For cache of Graphviz files and rendered images, use this directory
            final var cacheDir = Files.createTempDirectory("vcat-cache");
            // Temporary directory for Graphviz files and rendered images
            final var tempDir = Files.createTempDirectory("vcat-temp");

            Files.createDirectories(cacheDir);
            Files.createDirectories(tempDir);

            final var graphvizDirPath = Paths.get(graphvizDir);
            final var baseGraphviz = new GraphvizExternal(graphvizDirPath);
            final var graphviz = new QueuedGraphviz(baseGraphviz, graphvizThreads);

            // Create renderer
            vCatRenderer = new QueuedVCatRenderer(
                    new CachedVCatRenderer(graphviz, tempDir, apiClient, cacheDir, cachePurge),
                    vcatThreads);

        } catch (IOException | VCatException e) {
            throw new ServletException(e);
        }

    }

    @GET
    public Response render(@Context UriInfo uriInfo) {
        try {
            if (vCatRenderer.getNumberOfQueuedJobs() > vcatQueue) {
                return errorResponse(Response.Status.TOO_MANY_REQUESTS,
                        Messages.getString("ToolforgeVCatServlet.Error.TooManyQueuedJobs"));
            }

            final var pathParameters = convertRestParameters(uriInfo.getQueryParameters());
            final var renderedFileInfo = vCatRenderer.render(
                    new AllParamsToolforge(pathParameters, uriStringWithoutQuery(uriInfo), metadataProvider,
                            toolforgeWikiProvider));

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
        } catch (Exception e) {
            LOG.error("Error rendering response", e);
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

}
