package org.toolforge.vcat.graphviz.interfaces;

import org.toolforge.vcat.graphviz.GraphvizException;
import org.toolforge.vcat.params.GraphvizParams;

import java.nio.file.Path;

/**
 * Interface for a generic Graphviz renderer.
 *
 * @author Peter Schlömer
 */
public interface Graphviz {

    void render(Path inputFile, Path outputFile, GraphvizParams params) throws GraphvizException;

}
