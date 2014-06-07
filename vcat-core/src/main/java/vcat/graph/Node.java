package vcat.graph;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import vcat.Messages;
import vcat.graph.internal.GraphProperty;

public class Node extends DefaultNode {

	private String href;

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
	public boolean equals(Object o) {
		if (o instanceof Node) {
			// two nodes are equal if they have the same name
			return this.name.equals(((Node) o).name);
		} else {
			return false;
		}
	}

	@GraphProperty(Graph.PROPERTY_LABEL)
	public String getLabel() {
		return this.label;
	}

	public String getName() {
		return this.name;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(197, 1117).append(this.name).toHashCode();
	}

	public void setHref(String href) {
		this.href = href;
	}

	public void setLabel(String label) {
		this.label = label;
	}

}
