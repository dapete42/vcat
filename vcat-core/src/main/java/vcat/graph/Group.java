package vcat.graph;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import vcat.graph.internal.AbstractGraphPropertyUser;
import vcat.graph.internal.GraphProperty;
import vcat.graphviz.Graphviz;

public class Group extends AbstractGraphPropertyUser {

	private final DefaultEdge defaultEdge = new DefaultEdge();

	private final DefaultNode defaultNode = new DefaultNode();

	private final String name;

	private final Set<Node> nodes = new HashSet<Node>();

	private GroupRank rank = GroupRank.none;

	public Group(String name) {
		this.name = name;
	}

	public void addNode(Node node) {
		this.nodes.add(node);
	}

	public void addNodes(Collection<Node> node) {
		this.nodes.addAll(node);
	}

	public DefaultEdge getDefaultEdge() {
		return defaultEdge;
	}

	public DefaultNode getDefaultNode() {
		return defaultNode;
	}

	public String getName() {
		return this.name;
	}

	public Set<Node> getNodes() {
		return this.nodes;
	}

	@GraphProperty(Graphviz.PROPERTY_RANK)
	public GroupRank getRank() {
		return this.rank;
	}

	public void setRank(GroupRank rank) {
		this.rank = rank;
	}

}
