package org.toolforge.vcat.toolforge.webapp.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jspecify.annotations.Nullable;
import org.toolforge.vcat.AbstractVCat;
import org.toolforge.vcat.graph.Graph;
import org.toolforge.vcat.graphviz.GraphWriter;
import org.toolforge.vcat.graphviz.interfaces.Graphviz;
import org.toolforge.vcat.params.Algorithm;
import org.toolforge.vcat.params.GraphvizParams;
import org.toolforge.vcat.params.OutputFormat;

import java.io.IOException;
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

        Path graphvizFile = null;
        Path resultFile = null;
        try {
            graphvizFile = Files.createTempFile("testFontRendering", ".gv");
            final var graph = new Graph();
            graph.getDefaultNode().setFontname(AbstractVCat.GRAPH_FONT);
            graph.getDefaultNode().setShape("none");
            graph.node("default").setLabel(text);
            GraphWriter.writeGraphToFile(graph, graphvizFile);

            resultFile = Files.createTempFile("testFontRendering", ".png");

            final var graphvizParams = new GraphvizParams();
            graphvizParams.setAlgorithm(Algorithm.DOT);
            graphvizParams.setOutputFormat(OutputFormat.PNG);

            graphviz.render(graphvizFile, resultFile, graphvizParams);

            final String mimeType = graphvizParams.getOutputFormat().getMimeType();

            LOG.info("Sending file '{}' as '{}'", resultFile.toAbsolutePath(), mimeType);
            return Response.ok(Files.newInputStream(resultFile))
                    .type(mimeType)
                    .build();
        } catch (Exception e) {
            LOG.error("Error rendering response", e);
            return responseService.errorResponse(Response.Status.INTERNAL_SERVER_ERROR, e);
        } finally {
            deleteTempFile(graphvizFile);
            deleteTempFile(resultFile);
        }
    }

    private static void deleteTempFile(@Nullable Path tempFile) {
        if (tempFile != null) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                // ignore
            }
        }
    }

}
