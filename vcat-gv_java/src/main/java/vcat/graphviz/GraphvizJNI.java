/*
 * Copyright (C) 2013 Peter Schlömer
 * Released unter the Eclipse Public License - v 1.0.
 */

package vcat.graphviz;

import java.io.File;

import org.graphviz.SWIGTYPE_p_Agraph_t;
import org.graphviz.gv;

import vcat.graphviz.Graphviz;
import vcat.graphviz.GraphvizException;
import vcat.params.GraphvizParams;

/**
 * Graphviz renderer which uses the Graphviz JNI.
 * 
 * @author Peter Schlömer
 */
public class GraphvizJNI implements Graphviz {

	static {
		GraphvizJNILoader.init();
	}

	@Override
	public void render(File inputFile, File outputFile, GraphvizParams params) throws GraphvizException {
		// Read graph data from file
		SWIGTYPE_p_Agraph_t graph = gv.read(inputFile.getAbsolutePath());

		if (graph == null) {
			throw new GraphvizException("Error running gv.read for input file '" + inputFile.getAbsolutePath()
					+ "': No graph returned");
		}

		// Try to layout using one of Graphviz' algorithms. The layouting should not be parallelized, so we synchronize
		// to the gv class.
		synchronized (gv.class) {
			if (!gv.layout(graph, params.getAlgorithm().getProgram())) {
				// cleanup
				gv.rm(graph);
				throw new GraphvizException("Error running gv.layout on graph read from input file '"
						+ inputFile.getAbsolutePath() + "'");
			}
		}

		// Try to render layouted graph to file in output format
		if (!gv.render(graph, params.getOutputFormat().getGraphvizTypeParameter(), outputFile.getAbsolutePath())) {
			// cleanup
			gv.rm(graph);
			throw new GraphvizException("Error running gv.render for graph read from input file '"
					+ inputFile.getAbsolutePath() + "'");
		}

		// Remove graph object
		gv.rm(graph);
	}
}
