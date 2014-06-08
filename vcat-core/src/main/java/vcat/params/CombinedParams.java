package vcat.params;

import java.io.Serializable;

import vcat.mediawiki.IWiki;

/**
 * Combined parameters for the graph generation ({@link VCatParams}) and parameters for the invocation of the Graphviz
 * command line tool ({@link GraphvizParams}). Together these are a unique definition for each image rendered by
 * Graphviz.
 * 
 * @author schloeme
 */
public class CombinedParams<W extends IWiki> implements Serializable {

	private static final long serialVersionUID = -6156218131952150941L;

	private VCatParams<W> vCatParams;

	private GraphvizParams graphvizParams;

	protected CombinedParams() {
		this.vCatParams = new VCatParams<>();
		this.graphvizParams = new GraphvizParams();
	}

	public CombinedParams(VCatParams<W> vCatParams, GraphvizParams graphvizParams) {
		this.vCatParams = vCatParams;
		this.graphvizParams = graphvizParams;
	}

	public VCatParams<W> getVCat() {
		return this.vCatParams;
	}

	public GraphvizParams getGraphviz() {
		return this.graphvizParams;
	}

}
