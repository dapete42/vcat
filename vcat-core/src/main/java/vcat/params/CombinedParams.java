package vcat.params;

import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;

/**
 * Combined parameters for the graph generation ({@link VCatParams}) and parameters for the invocation of the Graphviz
 * command line tool ({@link GraphvizParams}). Together these are a unique definition for each image rendered by
 * Graphviz.
 *
 * @author schloeme
 */
@Getter
public class CombinedParams implements Serializable {

    @Serial
    private static final long serialVersionUID = -6156218131952150941L;

    private final VCatParams vCat;

    private final GraphvizParams graphviz;

    protected CombinedParams() {
        vCat = new VCatParams();
        graphviz = new GraphvizParams();
    }

    public CombinedParams(VCatParams vCatParams, GraphvizParams graphvizParams) {
        vCat = vCatParams;
        graphviz = graphvizParams;
    }

}
