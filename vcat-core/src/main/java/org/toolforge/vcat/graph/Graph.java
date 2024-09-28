package org.toolforge.vcat.graph;

import lombok.Getter;
import lombok.Setter;
import org.toolforge.vcat.graph.internal.AbstractHasGraphProperties;

import java.util.*;

public class Graph extends AbstractHasGraphProperties {

    public final static String EDGE = "edge";

    public final static String NODE = "node";

    public final static String PROPERTY_FONTNAME = "fontname";

    public final static String PROPERTY_FONTSIZE = "fontsize";

    public final static String PROPERTY_HREF = "href";

    public final static String PROPERTY_LABEL = "label";

    public final static String PROPERTY_RANK = "rank";

    public final static String PROPERTY_SHAPE = "shape";

    public final static String PROPERTY_SPLINES = "splines";

    public final static String PROPERTY_STYLE = "style";

    public final static String SHAPE_RECT = "rect";

    public final static String STYLE_BOLD = "bold";

    public final static String STYLE_DASHED = "dashed";

    public final static String TRUE = "true";

    @Getter
    private final DefaultEdge defaultEdge = new DefaultEdge();

    @Getter
    private final DefaultNode defaultNode = new DefaultNode();

    private final Set<Edge> edgeSet = new HashSet<>();

    @Getter
    @Setter
    private String fontname;

    @Getter
    @Setter
    private int fontsize;

    private final Map<String, Group> groupMap = new HashMap<>();

    private final Map<String, Node> nodeMap = new HashMap<>();

    @Getter
    @Setter
    private boolean splines = false;

    @Getter
    @Setter
    private String label;

    public boolean containsNode(String name) {
        return nodeMap.containsKey(name);
    }

    /**
     * Return and, if it does not exist yet, create an Edge between two Nodes in this Graph.
     *
     * @param nodeFrom from Node
     * @param nodeTo   to Node
     * @return Edge between
     */
    public Edge edge(Node nodeFrom, Node nodeTo) {
        final var edge = new Edge(nodeFrom, nodeTo);
        edgeSet.add(edge);
        return edge;
    }

    public Edge edgeReverse(Node nodeTo, Node nodeFrom) {
        return edge(nodeFrom, nodeTo);
    }

    public Set<Edge> getEdges() {
        return Collections.unmodifiableSet(edgeSet);
    }

    public Set<Edge> getEdgesFrom(Node node) {
        final Set<Edge> edges = new HashSet<>();
        for (Edge edge : edgeSet) {
            if (edge.getNodeFrom().equals(node)) {
                edges.add(edge);
            }
        }
        return edges;
    }

    public Set<Edge> getEdgesTo(Node node) {
        final Set<Edge> edges = new HashSet<>();
        for (Edge edge : edgeSet) {
            if (edge.getNodeTo().equals(node)) {
                edges.add(edge);
            }
        }
        return edges;
    }

    @Override
    protected SortedMap<String, String> propertiesInternal() {
        final SortedMap<String, String> properties = new TreeMap<>();
        properties.put(PROPERTY_FONTNAME, fontname);
        if (fontsize != 0) {
            properties.put(PROPERTY_FONTSIZE, Integer.toString(fontsize));
        }
        properties.put(PROPERTY_LABEL, label);
        if (splines) {
            properties.put(PROPERTY_SPLINES, TRUE);
        }
        return properties;
    }

    public Collection<Group> getGroups() {
        return Collections.unmodifiableCollection(groupMap.values());
    }

    public int getNodeCount() {
        return nodeMap.size();
    }

    public Collection<Node> getNodes() {
        return Collections.unmodifiableCollection(nodeMap.values());
    }

    public Group group(String name) {
        var group = groupMap.get(name);
        if (group == null) {
            group = new Group(name);
            groupMap.put(name, group);
        }
        return group;
    }

    public Node node(String name) {
        var node = nodeMap.get(name);
        if (node == null) {
            node = new Node(name);
            nodeMap.put(name, node);
        }
        return node;
    }

}
