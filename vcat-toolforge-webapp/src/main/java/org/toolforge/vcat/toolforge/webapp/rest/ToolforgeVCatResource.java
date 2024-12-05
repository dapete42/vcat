package org.toolforge.vcat.toolforge.webapp.rest;

import io.quarkus.qute.Template;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.toolforge.vcat.VCatException;
import org.toolforge.vcat.mediawiki.interfaces.MetadataProvider;
import org.toolforge.vcat.renderer.QueuedVCatRenderer;
import org.toolforge.vcat.renderer.interfaces.VCatRenderer;
import org.toolforge.vcat.toolforge.webapp.AllParamsToolforge;
import org.toolforge.vcat.toolforge.webapp.Messages;
import org.toolforge.vcat.toolforge.webapp.ToolforgeWikiProvider;
import org.toolforge.vcat.toolforge.webapp.cdi.qualifier.MetadataProviderQualifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Path("/render")
public class ToolforgeVCatResource {

    @Inject
    UriInfo uriInfo;

    /**
     * Maximum number of processes to queue vor vCat
     */
    @Inject
    @ConfigProperty(name = "vcat.queue", defaultValue = "100")
    Integer vcatQueue;

    /**
     * Cute error template
     */
    @Inject
    Template error;

    @Inject
    @MetadataProviderQualifier
    MetadataProvider metadataProvider;

    @Inject
    ToolforgeWikiProvider toolforgeWikiProvider;

    @Inject
    VCatRenderer vCatRenderer;

    private Response errorResponse(Response.Status status, String message) {
        return errorResponse(status, message, "");
    }

    private Response errorResponse(Response.Status status, Exception e) {
        var stacktrace = Stream.of(e.getStackTrace())
                .map(StackTraceElement::toString)
                .collect(Collectors.joining("\n"));
        return errorResponse(status, e.getMessage(), stacktrace);
    }

    private Response errorResponse(Response.Status status, String errorMessage, String stacktrace) {
        return Response.ok()
                .entity(
                        error.data(
                                "status", status,
                                "errorMessage", errorMessage,
                                "stacktrace", stacktrace
                        ).render().getBytes(StandardCharsets.UTF_8)
                )
                .type(MediaType.TEXT_HTML_TYPE)
                .status(status)
                .build();
    }

    private static String uriStringWithoutQuery(UriInfo uriInfo) {
        return uriInfo.getRequestUriBuilder().replaceQuery(null).build().toString();
    }

    @GET
    public Response render() {
        try {
            if (vCatRenderer instanceof QueuedVCatRenderer queuedVCatRenderer
                && queuedVCatRenderer.getQueueLength() > vcatQueue) {
                return errorResponse(Response.Status.TOO_MANY_REQUESTS,
                        Messages.getString("ToolforgeVCatServlet.Error.TooManyQueuedJobs"));
            }

            final var renderedFileInfo = vCatRenderer.render(
                    new AllParamsToolforge(uriInfo.getQueryParameters(), uriStringWithoutQuery(uriInfo), metadataProvider,
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
            return errorResponse(Response.Status.BAD_REQUEST, e);
        } catch (Exception e) {
            LOG.error("Error rendering response", e);
            return errorResponse(Response.Status.INTERNAL_SERVER_ERROR, e);
        }
    }

}
