package org.toolforge.vcat.graphviz;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import org.toolforge.vcat.graphviz.interfaces.Graphviz;
import org.toolforge.vcat.params.GraphvizParams;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class GraphvizJna implements Graphviz {

    public interface CGraph extends Library {
        CGraph INSTANCE = Native.load("cgraph", CGraph.class);

        Pointer agmemread(String cp);

        int agclose(Pointer g);
    }

    public interface GVC extends Library {
        GVC INSTANCE = Native.load("gvc", GVC.class);

        Pointer gvContext();

        int gvLayout(Pointer gvc, Pointer g, String engine);

        int gvRenderFilename(Pointer gvc, Pointer g, String format, String filename);

        int gvFreeLayout(Pointer gvc, Pointer g);

        int gvFreeContext(Pointer gvc);
    }

    @Override
    public void render(Path inputFile, Path outputFile, GraphvizParams params) throws GraphvizException {
        final String dotContent;
        try {
            dotContent = Files.readString(inputFile);
        } catch (Exception e) {
            throw new GraphvizException(e);
        }
        final String outputPath = outputFile.toAbsolutePath().toString();
        final String format = Objects.requireNonNull(params.getOutputFormat().getGraphvizTypeParameter());

        // 1. Create Context
        Pointer gvc = GVC.INSTANCE.gvContext();

        // 2. Read Graph from String
        Pointer g = CGraph.INSTANCE.agmemread(dotContent);

        try {
            GVC.INSTANCE.gvLayout(gvc, g, params.getAlgorithm().getProgram());
            GVC.INSTANCE.gvRenderFilename(gvc, g, format, outputPath);
            GVC.INSTANCE.gvFreeLayout(gvc, g);
        } finally {
            CGraph.INSTANCE.agclose(g);
            GVC.INSTANCE.gvFreeContext(gvc);
        }
    }

}
