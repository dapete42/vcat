package org.toolforge.vcat.toolforge.webapp.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.toolforge.vcat.graphviz.interfaces.Graphviz;
import org.toolforge.vcat.toolforge.webapp.util.TestFontRenderingHelper;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@jakarta.ws.rs.Path("/test-font-rendering")
public class TestFontRenderingResource {

    @Inject
    @ConfigProperty(name = "testFontRendering.enabled", defaultValue = "false")
    boolean testFontRenderingEnabled;

    @Inject
    Graphviz graphviz;

    @Inject
    ResponseService responseService;

    @GET
    public Response testFontRendering(@QueryParam("text") String text) {
        if (!testFontRenderingEnabled) {
            return responseService.errorResponse(Response.Status.FORBIDDEN, "testFontRendering.enabled must be true for this endpoint to respond");
        }

        Path imageFile = null;
        try {
            final var renderResult = TestFontRenderingHelper.renderImage(graphviz, text);
            imageFile = renderResult.imageFile();
            String mimeType = renderResult.outputFormat().getMimeType();

            LOG.info("Sending file '{}' as '{}'", imageFile.toAbsolutePath(), mimeType);
            return Response.ok(Files.newInputStream(imageFile))
                    .type(mimeType)
                    .build();
        } catch (Exception e) {
            LOG.error("Error rendering response", e);
            return responseService.errorResponse(Response.Status.INTERNAL_SERVER_ERROR, e);
        } finally {
            TestFontRenderingHelper.deleteTempFile(imageFile);
        }
    }


}
