package org.toolforge.vcat.webapp.simple.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import lombok.extern.slf4j.Slf4j;
import org.toolforge.vcat.VCatException;
import org.toolforge.vcat.mediawiki.interfaces.MetadataProvider;
import org.toolforge.vcat.params.AllParams;
import org.toolforge.vcat.renderer.interfaces.VCatRenderer;

import java.io.IOException;
import java.nio.file.Files;
import java.util.StringJoiner;
import java.util.stream.Stream;

@Slf4j
@Path("/render")
public class SimpleVCatResource {

    @Inject
    MetadataProvider metadataProvider;

    @Inject
    VCatRenderer vCatRenderer;

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
