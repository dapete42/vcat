package vcat.params;

import java.io.Serializable;

import org.apache.commons.lang3.builder.HashCodeBuilder;

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

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		} else if (o instanceof GraphvizParams) {
			GraphvizParams gp = (GraphvizParams) o;
			return gp.algorithm == this.algorithm && gp.outputFormat == this.outputFormat;
		} else {
			return false;
		}
	}

	public Algorithm getAlgorithm() {
		return this.algorithm;
	}

	public OutputFormat getOutputFormat() {
		return this.outputFormat;
	}

	@Override
	public int hashCode() {
		HashCodeBuilder hcb = new HashCodeBuilder(71, 67);
		hcb.append(this.algorithm);
		hcb.append(this.outputFormat);
		return hcb.toHashCode();
	}

	public void setAlgorithm(Algorithm algorithm) {
		this.algorithm = algorithm;
	}

	public void setOutputFormat(OutputFormat outputFormat) {
		this.outputFormat = outputFormat;
		super.equals(null);
	}

}
