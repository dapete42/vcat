package vcat.graphviz;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import vcat.Messages;
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
		// Always escape strings, otherwise node names may be misidentified as GV keywords
		String output = string.replace("\\", "\\\\");
		output = output.replace("\r", "");
		output = output.replace("\n", "\\n");
		output = output.replace("\"", "\\\"");
		return '"' + output + '"';
	}

	public static void writeGraphToFile(Graph graph, File outputFile) throws GraphvizException {
		Writer writer = null;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile, false),
					StandardCharsets.UTF_8));
		} catch (FileNotFoundException e) {
			throw new GraphvizException(String.format(Messages.getString("GraphWriter.Exception.OpenFileForOutput"),
					outputFile.getAbsolutePath()), e);
		}

		GraphWriter graphWriter = new GraphWriter(writer);
		try {
			graphWriter.writeGraph(graph);
		} catch (IOException e) {
			throw new GraphvizException(String.format(Messages.getString("GraphWriter.Exception.WriteGraph"),
					outputFile.getAbsolutePath()), e);
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
			this.writer.write(Graph.EDGE);
			this.writer.write(' ');
			this.writeBracketProperties(properties);
			this.writer.write(';');
			this.writer.write('\n');
		}
	}

	private void writeDefaultNode(DefaultNode node) throws IOException {
		Map<String, String> properties = node.properties();
		if (!properties.isEmpty()) {
			this.writer.write(Graph.NODE);
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

		this.writer
				.write(String.format(Messages.getString("GraphWriter.Output.Header"), DATE_FORMAT.format(new Date())));

		this.writer.write("digraph cluster_vcat{\n");

		// Charset
		this.writer.write("charset=\"UTF-8\";\n");

		// Create a Set of nodes that have no edges; they have to be included even if they do not have any properties
		Set<Node> nodesWithoutEdges = new HashSet<>();
		// Assume no node as edges; then remove those that have from the list
		nodesWithoutEdges.addAll(graph.getNodes());
		for (Edge edge : graph.getEdges()) {
			nodesWithoutEdges.remove(edge.getNodeFrom());
			nodesWithoutEdges.remove(edge.getNodeTo());
		}

		this.writeLineProperties(graph.properties());

		this.writeDefaultNode(graph.getDefaultNode());
		this.writeDefaultEdge(graph.getDefaultEdge());

		for (Group group : graph.getGroups()) {
			this.writeGroup(group);
		}

		for (Node node : graph.getNodes()) {
			final Set<Edge> edges = graph.getEdgesFrom(node);
			// Write the node; if there are no edges, write it even if it has no properties
			this.writeNode(node, edges.isEmpty());
			for (Edge edge : edges) {
				this.writeEdge(edge);
			}
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
