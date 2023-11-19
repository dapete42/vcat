package org.toolforge.vcat.graph;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.toolforge.vcat.Messages;

import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

@Getter
@Setter
public class Node extends DefaultNode {

    private String label;

    private final String name;

    public Node(String name) {
        if (name == null) {
            throw new NullPointerException(Messages.getString("Node.Exception.NameNull"));
        }
        this.name = name;
    }

    public Node(String name, String label) {
        this(name);
        this.label = label;
    }

    @Override
    protected SortedMap<String, String> propertiesInternal() {
        final SortedMap<String, String> properties = new TreeMap<>(super.propertiesInternal());
        properties.put(Graph.PROPERTY_LABEL, label);
        return properties;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Node n) {
            // two nodes are equal if they have the same name
            return Objects.equals(name, n.name);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(197, 1117).append(name).toHashCode();
    }

}
