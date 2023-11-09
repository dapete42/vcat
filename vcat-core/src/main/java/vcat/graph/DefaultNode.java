package vcat.graph;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import vcat.graph.internal.AbstractDefaultEdgeNode;
import vcat.graph.internal.GraphProperty;

public class DefaultNode extends AbstractDefaultEdgeNode {

    private String shape;

    @GraphProperty(Graph.PROPERTY_SHAPE)
    public String getShape() {
        return this.shape;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DefaultNode n) {
            return new EqualsBuilder().appendSuper(super.equals(o)).append(shape, n.shape).build();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(197, 1117).append(shape).toHashCode();
    }

    public void setShape(String shape) {
        this.shape = shape;
    }

}
