package org.toolforge.vcat.toolforge.webapp.test;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jspecify.annotations.NonNull;
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

@ApplicationScoped
public class TestFontRendering {

    @Inject
    Graphviz graphviz;

    public byte @NonNull [] test(String text) throws GraphvizException, IOException {
        Path graphvizFile = null;
        Path resultFile = null;
        try {
            graphvizFile = Files.createTempFile("testFontRendering", ".gv");
            final var graph = new Graph();
            graph.getDefaultNode().setFontname(AbstractVCat.GRAPH_FONT);
            graph.getDefaultNode().setFontsize(40);
            graph.getDefaultNode().setShape("none");
            graph.node("default").setLabel(text);
            GraphWriter.writeGraphToFile(graph, graphvizFile);

            resultFile = Files.createTempFile("testFontRendering", ".png");

            final var graphvizParams = new GraphvizParams();
            graphvizParams.setAlgorithm(Algorithm.DOT);
            graphvizParams.setOutputFormat(OutputFormat.PNG);

            graphviz.render(graphvizFile, resultFile, graphvizParams);

            return Files.readAllBytes(resultFile);
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
