package org.toolforge.vcat.graph;

import lombok.Getter;
import lombok.Setter;
import org.toolforge.vcat.graph.internal.AbstractHasGraphProperties;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

@Getter
@Setter
public class Group extends AbstractHasGraphProperties {

    private final DefaultEdge defaultEdge = new DefaultEdge();

    private final DefaultNode defaultNode = new DefaultNode();

    private final String name;

    private final Set<Node> nodes = new HashSet<>();

    private GroupRank rank = GroupRank.none;

    public Group(String name) {
        this.name = name;
    }

    public void addNode(Node node) {
        nodes.add(node);
    }

    protected SortedMap<String, String> propertiesInternal() {
        final SortedMap<String, String> properties = new TreeMap<>();
        properties.put(Graph.PROPERTY_RANK, rank.name());
        return properties;
    }

}
