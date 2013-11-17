package vcat.graph;

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
	public int hashCode() {
		return new HashCodeBuilder(197, 1117).toHashCode();
	}

	public void setShape(String shape) {
		this.shape = shape;
	}

}
