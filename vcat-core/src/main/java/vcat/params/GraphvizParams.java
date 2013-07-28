package vcat.params;

import java.io.Serializable;

/**
 * Parameters for the invocation of the Graphviz command line tool used by the {@link vcat.graphviz.GraphvizExternal
 * Graphviz} class.
 * 
 * @author Peter Schlömer
 */
public class GraphvizParams implements Serializable {

	private static final long serialVersionUID = -6789163385179923066L;

	private Algorithm algorithm = Algorithm.DOT;

	private OutputFormat outputFormat = OutputFormat.PNG;

	public Algorithm getAlgorithm() {
		return this.algorithm;
	}

	public OutputFormat getOutputFormat() {
		return this.outputFormat;
	}

	public void setAlgorithm(Algorithm algorithm) {
		this.algorithm = algorithm;
	}

	public void setOutputFormat(OutputFormat outputFormat) {
		this.outputFormat = outputFormat;
	}

}
