package org.toolforge.vcat.params;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.toolforge.vcat.graphviz.GraphvizExternal;

import java.io.Serial;
import java.io.Serializable;

/**
 * Parameters for the invocation of the Graphviz command line tool used by the {@link GraphvizExternal
 * Graphviz} class.
 *
 * @author Peter Schlömer
 */
@Getter
@Setter
public class GraphvizParams implements Serializable {

    @Serial
    private static final long serialVersionUID = -3887996473000446247L;

    private Algorithm algorithm = Algorithm.DOT;

    private OutputFormat outputFormat = OutputFormat.PNG;

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (o instanceof GraphvizParams gp) {
            return gp.algorithm == this.algorithm && gp.outputFormat == this.outputFormat;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hcb = new HashCodeBuilder(71, 67);
        hcb.append(this.algorithm);
        hcb.append(this.outputFormat);
        return hcb.toHashCode();
    }

}
