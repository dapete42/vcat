package org.toolforge.vcat.toolforge.webapp.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
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

import java.nio.file.Files;

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

    @Inject
    @MetadataProviderQualifier
    MetadataProvider metadataProvider;

    @Inject
    ResponseService responseService;

    @Inject
    ToolforgeWikiProvider toolforgeWikiProvider;

    @Inject
    VCatRenderer vCatRenderer;

    private static String uriStringWithoutQuery(UriInfo uriInfo) {
        return uriInfo.getRequestUriBuilder().replaceQuery(null).build().toString();
    }

    @GET
    public Response render() {
        try {
            if (vCatRenderer instanceof QueuedVCatRenderer queuedVCatRenderer
                && queuedVCatRenderer.getQueueLength() > vcatQueue) {
                return responseService.errorResponse(Response.Status.TOO_MANY_REQUESTS,
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
            return responseService.errorResponse(Response.Status.BAD_REQUEST, e);
        } catch (Exception e) {
            LOG.error("Error rendering response", e);
            return responseService.errorResponse(Response.Status.INTERNAL_SERVER_ERROR, e);
        }
    }

}
