package vcat.graph;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import vcat.graph.internal.GraphProperty;

public class Edge extends DefaultEdge {

    private String label;

    private final Node nodeFrom;

    private final Node nodeTo;

    public Edge(Node nodeFrom, Node nodeTo) {
        this.nodeFrom = nodeFrom;
        this.nodeTo = nodeTo;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Edge) {
            Edge e = (Edge) o;
            // two nodes are equal if they go between the same nodes
            return this.nodeFrom.equals(e.nodeFrom) && this.nodeTo.equals(e.nodeTo);
        } else {
            return false;
        }
    }

    @GraphProperty(Graph.PROPERTY_LABEL)
    public String getLabel() {
        return this.label;
    }

    public Node getNodeFrom() {
        return this.nodeFrom;
    }

    public Node getNodeTo() {
        return this.nodeTo;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(113, 237).append(this.nodeFrom).append(this.nodeTo).toHashCode();
    }

    public void setLabel(String label) {
        this.label = label;
    }

}
