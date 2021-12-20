package vcat.graphviz;

import java.nio.file.Path;

import vcat.params.GraphvizParams;

/**
 * Interface for a generic Graphviz renderer.
 * 
 * @author Peter Schlömer
 */
public interface Graphviz {

	public abstract void render(Path inputFile, Path outputFile, GraphvizParams params) throws GraphvizException;

}