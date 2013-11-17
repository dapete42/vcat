package vcat.graph;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import vcat.graph.internal.AbstractGraphPropertyUser;
import vcat.graph.internal.GraphProperty;

public class Graph extends AbstractGraphPropertyUser {

	/**
	 * Pattern do determine if a string can be used as an identifier in a graphviz file without quoting.
	 */
	public final static Pattern PATTERN_IDENTIFIER = Pattern.compile("[A-Za-z][A-Za-z0-9_]+");

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

	public static Graph createSubGraph(Graph fullGraph, Node rootNode, int depth) {
		Graph subGraph = new Graph();
		Set<Node> rootNodeSet = Collections.singleton(rootNode);
		createSubgraphRecursive(subGraph, fullGraph, rootNodeSet, depth);
		return subGraph;
	}

	public static void createSubgraphRecursive(Graph subGraph, Graph fullGraph, Set<Node> nodes, int depth) {
		if (depth > 0) {
			HashSet<Node> nextNodes = new HashSet<Node>();
			for (Node node : nodes) {
				subGraph.nodeMap.put(node.getName(), node);
				for (Edge edge : fullGraph.getEdgesFrom(node)) {
					Node nodeTo = edge.getNodeTo();
					subGraph.nodeMap.put(nodeTo.getName(), nodeTo);
					subGraph.edgeSet.add(edge);
					nextNodes.add(nodeTo);
				}
				for (Edge edge : fullGraph.getEdgesTo(node)) {
					Node nodeFrom = edge.getNodeTo();
					subGraph.nodeMap.put(nodeFrom.getName(), nodeFrom);
					subGraph.edgeSet.add(edge);
					nextNodes.add(nodeFrom);
				}
			}
			createSubgraphRecursive(subGraph, fullGraph, nextNodes, depth - 1);
		} else {

		}
	}

	private final DefaultEdge defaultEdge = new DefaultEdge();

	private final DefaultNode defaultNode = new DefaultNode();

	private final Set<Edge> edgeSet = new HashSet<Edge>();

	private String fontname;

	private int fontsize;

	private final Map<String, Group> groupMap = new HashMap<String, Group>();

	private final Map<String, Node> nodeMap = new HashMap<String, Node>();

	private boolean splines = false;

	private String label;

	public boolean containsNode(String name) {
		return this.nodeMap.containsKey(name);
	}

	/**
	 * Return and, if it does not exist yet, create an Edge between two Nodes in this Graph.
	 * 
	 * @param nodeFrom
	 *            from Node
	 * @param nodeTo
	 *            to Node
	 * @return Edge between
	 */
	public Edge edge(Node nodeFrom, Node nodeTo) {
		Edge edge = new Edge(nodeFrom, nodeTo);
		this.edgeSet.add(edge);
		return edge;
	}

	public DefaultEdge getDefaultEdge() {
		return defaultEdge;
	}

	public DefaultNode getDefaultNode() {
		return defaultNode;
	}

	public Set<Edge> getEdges() {
		return Collections.unmodifiableSet(this.edgeSet);
	}

	public Set<Edge> getEdgesFrom(Node node) {
		Set<Edge> edges = new HashSet<Edge>();
		for (Edge edge : this.edgeSet) {
			if (edge.getNodeFrom().equals(node)) {
				edges.add(edge);
			}
		}
		return edges;
	}

	public Set<Edge> getEdgesTo(Node node) {
		Set<Edge> edges = new HashSet<Edge>();
		for (Edge edge : this.edgeSet) {
			if (edge.getNodeTo().equals(node)) {
				edges.add(edge);
			}
		}
		return edges;
	}

	@GraphProperty(PROPERTY_FONTNAME)
	public String getFontname() {
		return this.fontname;
	}

	public int getFontsize() {
		return this.fontsize;
	}

	@GraphProperty(PROPERTY_FONTSIZE)
	public String getFontsizeString() {
		if (this.fontsize == 0) {
			return null;
		} else {
			return Integer.toString(this.fontsize);
		}
	}

	public Collection<Group> getGroups() {
		return Collections.unmodifiableCollection(this.groupMap.values());
	}

	@GraphProperty(PROPERTY_LABEL)
	public String getLabel() {
		return this.label;
	}

	public int getNodeCount() {
		return this.nodeMap.size();
	}

	public Collection<Node> getNodes() {
		return Collections.unmodifiableCollection(this.nodeMap.values());
	}

	@GraphProperty(PROPERTY_SPLINES)
	public String getSplinesString() {
		if (this.splines) {
			return TRUE;
		} else {
			return null;
		}
	}

	public Group group(String name) {
		Group group = this.groupMap.get(name);
		if (group == null) {
			group = new Group(name);
			this.groupMap.put(name, group);
		}
		return group;
	}

	public boolean isSplines() {
		return this.splines;
	}

	public Node node(String name) {
		Node node = this.nodeMap.get(name);
		if (node == null) {
			node = new Node(name);
			this.nodeMap.put(name, node);
		}
		return node;
	}

	public void setFontname(String fontname) {
		this.fontname = fontname;
	}

	public void setFontsize(int fontsize) {
		this.fontsize = fontsize;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setSplines(boolean splines) {
		this.splines = splines;
	}

}
