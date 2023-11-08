package vcat.graphviz.interfaces;

import vcat.graphviz.GraphvizException;
import vcat.params.GraphvizParams;

import java.nio.file.Path;

/**
 * Interface for a generic Graphviz renderer.
 *
 * @author Peter Schlömer
 */
public interface Graphviz {

    public abstract void render(Path inputFile, Path outputFile, GraphvizParams params) throws GraphvizException;

}