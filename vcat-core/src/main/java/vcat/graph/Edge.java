package vcat.graph;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

@Getter
@Setter
public class Edge extends DefaultEdge {

    private String label;

    private final Node nodeFrom;

    private final Node nodeTo;

    public Edge(Node nodeFrom, Node nodeTo) {
        this.nodeFrom = nodeFrom;
        this.nodeTo = nodeTo;
    }

    @Override
    protected SortedMap<String, String> propertiesInternal() {
        final SortedMap<String, String> properties = new TreeMap<>(super.propertiesInternal());
        properties.put(Graph.PROPERTY_LABEL, label);
        return properties;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Edge e) {
            // two nodes are equal if they go between the same nodes
            return Objects.equals(nodeFrom, e.nodeFrom) && Objects.equals(nodeTo, e.nodeTo);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(113, 237).append(nodeFrom).append(nodeTo).toHashCode();
    }

}
