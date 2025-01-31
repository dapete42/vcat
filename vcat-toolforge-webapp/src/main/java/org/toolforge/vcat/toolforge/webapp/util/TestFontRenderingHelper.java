package org.toolforge.vcat.toolforge.webapp.util;

import org.jspecify.annotations.Nullable;
import org.toolforge.vcat.AbstractVCat;
import org.toolforge.vcat.graph.Graph;
import org.toolforge.vcat.graphviz.GraphWriter;
import org.toolforge.vcat.graphviz.GraphvizException;
import org.toolforge.vcat.graphviz.interfaces.Graphviz;
import org.toolforge.vcat.params.Algorithm;
import org.toolforge.vcat.params.GraphvizParams;
import org.toolforge.vcat.params.OutputFormat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TestFontRenderingHelper {

    public record RenderImageResult(Path imageFile, OutputFormat outputFormat) {
    }

    private TestFontRenderingHelper() {
    }

    public static void deleteTempFile(@Nullable Path tempFile) {
        if (tempFile != null) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public static RenderImageResult renderImage(Graphviz graphviz, String text) throws GraphvizException, IOException {

        Path graphvizFile = null;
        try {
            graphvizFile = Files.createTempFile("TestFontRenderingHelper", ".gv");

            final var graph = new Graph();
            graph.setFontname(AbstractVCat.GRAPH_FONT);
            graph.getDefaultNode().setFontname(AbstractVCat.GRAPH_FONT);
            graph.getDefaultNode().setShape("none");
            graph.node("default").setLabel(text);
            GraphWriter.writeGraphToFile(graph, graphvizFile);

            final var resultFile = Files.createTempFile("TestFontRenderingHelper", ".png");

            final var graphvizParams = new GraphvizParams();
            graphvizParams.setAlgorithm(Algorithm.DOT);
            graphvizParams.setOutputFormat(OutputFormat.PNG);
            graphviz.render(graphvizFile, resultFile, graphvizParams);

            return new RenderImageResult(resultFile, graphvizParams.getOutputFormat());
        } finally {
            deleteTempFile(graphvizFile);
        }

    }

}
