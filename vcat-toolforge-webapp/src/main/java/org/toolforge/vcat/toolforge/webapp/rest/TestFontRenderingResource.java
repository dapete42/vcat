package org.toolforge.vcat.toolforge.webapp.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.toolforge.vcat.params.OutputFormat;
import org.toolforge.vcat.toolforge.webapp.test.TestFontRendering;

@Slf4j
@jakarta.ws.rs.Path("/test-font-rendering")
public class TestFontRenderingResource {

    @Inject
    @ConfigProperty(name = "testFontRendering.enabled", defaultValue = "false")
    boolean testFontRenderingEnabled;

    @Inject
    ResponseService responseService;

    @Inject
    TestFontRendering testFontRendering;

    @GET
    public Response testFontRendering(@QueryParam("text") String text) {
        if (!testFontRenderingEnabled) {
            return responseService.errorResponse(Response.Status.FORBIDDEN, "testFontRendering.enabled must be true for this endpoint to respond");
        }

        try {
            final byte[] resultBytes = testFontRendering.test(text);
            LOG.info("Sending image file for text '{}'", text);
            return Response.ok(resultBytes)
                    .type(OutputFormat.PNG.getMimeType())
                    .build();
        } catch (Exception e) {
            LOG.error("Error rendering response", e);
            return responseService.errorResponse(Response.Status.INTERNAL_SERVER_ERROR, e);
        }
    }

}
