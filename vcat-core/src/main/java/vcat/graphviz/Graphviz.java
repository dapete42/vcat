package vcat.graphviz;

import java.io.File;

import vcat.params.GraphvizParams;

/**
 * Interface for a generic Graphviz renderer.
 * 
 * @author Peter Schlömer
 */
public interface Graphviz {

	public abstract void render(File inputFile, File outputFile, GraphvizParams params) throws GraphvizException;

}