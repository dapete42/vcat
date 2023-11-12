package vcat.graph;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import vcat.graph.internal.AbstractDefaultEdgeNode;

import java.util.SortedMap;
import java.util.TreeMap;

@Getter
@Setter
public class DefaultNode extends AbstractDefaultEdgeNode {

    private String shape;

    @Override
    protected SortedMap<String, String> propertiesInternal() {
        final SortedMap<String, String> properties = new TreeMap<>(super.propertiesInternal());
        properties.put(Graph.PROPERTY_SHAPE, shape);
        return properties;
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

}
