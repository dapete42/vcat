package vcat.graphviz;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import vcat.graph.DefaultEdge;
import vcat.graph.DefaultNode;
import vcat.graph.Edge;
import vcat.graph.Graph;
import vcat.graph.Group;
import vcat.graph.Node;

/**
 * Write the contents of a {@link Graph} to a {@link Writer} as a Graphviz file (<code>.gv</code>, formerly
 * <code>.dot</code>).
 * 
 * @author Peter Schlömer
 */
public class GraphWriter {

	private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

	private static String escape(String string) {
		// Simple text strings do not have to be escaped
		if (Graphviz.PATTERN_IDENTIFIER.matcher(string).matches()) {
			return string;
		} else {
			String output = string.replace("\\", "\\\\");
			output = output.replace("\r", "");
			output = output.replace("\n", "\\n");
			output = output.replace("\"", "\\\"");
			return "\"" + output + "\"";
		}
	}

	public static void writeGraphToFile(Graph graph, File outputFile) throws GraphvizException {
		Writer writer = null;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile, false), "UTF8"));
		} catch (UnsupportedEncodingException e) {
			throw new GraphvizException("UTF8 encoding must be supported to create Graphviz output files", e);
		} catch (FileNotFoundException e) {
			throw new GraphvizException("Error opening file '" + outputFile.getAbsolutePath() + "' for output", e);
		}

		GraphWriter graphWriter = new GraphWriter(writer);
		try {
			graphWriter.writeGraph(graph);
		} catch (IOException e) {
			throw new GraphvizException("Error writing graph", e);
		}
	}

	/** The Writer the {@link Graph} will be written to. */
	private final Writer writer;

	/**
	 * Create a new GraphWriter.
	 * 
	 * @param writer
	 *            The Writer the {@link Graph} will be written to.
	 */
	public GraphWriter(Writer writer) {
		this.writer = writer;
	}

	private void writeBracketProperties(Map<String, String> properties) throws IOException {
		if (!properties.isEmpty()) {
			this.writer.write('[');
			boolean first = true;
			for (Entry<String, String> property : properties.entrySet()) {
				if (first) {
					first = false;
				} else {
					this.writer.write(',');
				}
				this.writer.write(property.getKey());
				this.writer.write('=');
				String value = property.getValue();
				if (value == null) {
					this.writer.write('"');
					this.writer.write('"');
				} else {
					this.writer.write(escape(value));
				}
			}
			this.writer.write(']');
		}
	}

	private void writeDefaultEdge(DefaultEdge edge) throws IOException {
		Map<String, String> properties = edge.properties();
		if (!properties.isEmpty()) {
			this.writer.write(Graphviz.EDGE);
			this.writer.write(' ');
			this.writeBracketProperties(properties);
			this.writer.write(';');
			this.writer.write('\n');
		}
	}

	private void writeDefaultNode(DefaultNode node) throws IOException {
		Map<String, String> properties = node.properties();
		if (!properties.isEmpty()) {
			this.writer.write(Graphviz.NODE);
			this.writer.write(' ');
			this.writeBracketProperties(properties);
			this.writer.write(';');
			this.writer.write('\n');
		}
	}

	private void writeEdge(Edge edge) throws IOException {
		this.writer.write(escape(edge.getNodeFrom().getName()));
		this.writer.write(" -> ");
		this.writer.write(escape(edge.getNodeTo().getName()));
		Map<String, String> properties = edge.properties();
		if (!properties.isEmpty()) {
			this.writer.write(' ');
			this.writeBracketProperties(properties);
		}
		this.writer.write(';');
		this.writer.write('\n');
	}

	public void writeGraph(Graph graph) throws IOException {

		this.writer.write("// Created by GraphWriter, ");
		this.writer.write(DATE_FORMAT.format(new Date()));
		this.writer.write('\n');

		this.writer.write("digraph cluster_vcat{\n");

		// Charset
		this.writer.write("charset=\"UTF-8\";\n");

		// Create a Set of nodes that have no edges; they have to be included even if they do not have any properties
		Set<Node> nodesWithoutEdges = new HashSet<Node>();
		// Assume no node as edges; then remove those that have from the list
		nodesWithoutEdges.addAll(graph.getNodes());
		for (Edge edge : graph.getEdges()) {
			nodesWithoutEdges.remove(edge.getNodeFrom());
			nodesWithoutEdges.remove(edge.getNodeTo());
		}

		this.writeLineProperties(graph.properties());

		this.writeDefaultNode(graph.getDefaultNode());
		this.writeDefaultEdge(graph.getDefaultEdge());

		for (Node node : graph.getNodes()) {
			this.writeNode(node, nodesWithoutEdges.contains(node));
		}

		for (Group group : graph.getGroups()) {
			this.writeGroup(group);
		}

		for (Edge edge : graph.getEdges()) {
			this.writeEdge(edge);
		}

		this.writer.write('}');
		this.writer.write('\n');

		this.writer.close();
	}

	private void writeGroup(Group group) throws IOException {
		this.writer.write("{\n");
		this.writeLineProperties(group.properties());
		writeDefaultNode(group.getDefaultNode());
		writeDefaultEdge(group.getDefaultEdge());
		for (Node node : group.getNodes()) {
			this.writer.write(escape(node.getName()));
			this.writer.write(' ');
		}
		this.writer.write('\n');
		this.writer.write('}');
		this.writer.write('\n');
	}

	private void writeLineProperties(Map<String, String> properties) throws IOException {
		if (!properties.isEmpty()) {
			for (Entry<String, String> property : properties.entrySet()) {
				writeLineProperty(property.getKey(), property.getValue());
			}
		}
	}

	private void writeLineProperty(String key, String value) throws IOException {
		this.writer.write(key);
		this.writer.write('=');
		if (value != null) {
			this.writer.write(escape(value));
		}
		this.writer.write(';');
		this.writer.write('\n');
	}

	private void writeNode(Node node, boolean writeWithoutProperties) throws IOException {
		Map<String, String> properties = node.properties();
		// Check if the "label" property is the same as the name; if it is, delete it
		if (node.getName().equals(properties.get("label"))) {
			properties.remove("label");
		}
		// Write node line only if now there are properties, unless overriden by parameter
		if (writeWithoutProperties || !properties.isEmpty()) {
			this.writer.write(escape(node.getName()));
			if (!properties.isEmpty()) {
				this.writer.write(' ');
				this.writeBracketProperties(properties);
			}
			this.writer.write(';');
			this.writer.write('\n');
		}
	}
}
