package org.toolforge.vcat.graphviz;

import org.jspecify.annotations.Nullable;
import org.slf4j.helpers.MessageFormatter;
import org.toolforge.vcat.Messages;
import org.toolforge.vcat.graph.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;

/**
 * Write the contents of a {@link Graph} to a {@link Writer} as a Graphviz file (<code>.gv</code>, formerly
 * <code>.dot</code>).
 *
 * @author Peter Schlömer
 */
public class GraphWriter {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

    private static String escape(String string) {
        // Always escape strings, otherwise node names may be misidentified as GV keywords
        String output = string.replace("\\", "\\\\");
        output = output.replace("\r", "");
        output = output.replace("\n", "\\n");
        output = output.replace("\"", "\\\"");
        return '"' + output + '"';
    }

    public static void writeGraphToFile(Graph graph, Path outputFile) throws GraphvizException {
        final Writer writer;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(
                    Files.newOutputStream(outputFile, StandardOpenOption.CREATE), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new GraphvizException(MessageFormatter
                    .format(Messages.getString("GraphWriter.Exception.OpenFileForOutput"), outputFile).getMessage(), e);
        }

        final var graphWriter = new GraphWriter(writer);
        try {
            graphWriter.writeGraph(graph);
        } catch (IOException e) {
            throw new GraphvizException(MessageFormatter
                    .format(Messages.getString("GraphWriter.Exception.WriteGraph"), outputFile).getMessage(), e);
        }
    }

    /**
     * The Writer the {@link Graph} will be written to.
     */
    private final Writer writer;

    /**
     * Create a new GraphWriter.
     *
     * @param writer The Writer the {@link Graph} will be written to.
     */
    public GraphWriter(Writer writer) {
        this.writer = writer;
    }

    private void writeBracketProperties(Map<String, String> properties) throws IOException {
        if (!properties.isEmpty()) {
            writer.write('[');
            boolean first = true;
            for (Entry<String, String> property : properties.entrySet()) {
                if (first) {
                    first = false;
                } else {
                    writer.write(',');
                }
                writer.write(property.getKey());
                writer.write('=');
                String value = property.getValue();
                if (value == null) {
                    writer.write('"');
                    writer.write('"');
                } else {
                    writer.write(escape(value));
                }
            }
            writer.write(']');
        }
    }

    private void writeDefaultEdge(DefaultEdge edge) throws IOException {
        final var properties = edge.properties();
        if (!properties.isEmpty()) {
            writer.write(Graph.EDGE);
            writer.write(' ');
            writeBracketProperties(properties);
            writer.write(';');
            writer.write('\n');
        }
    }

    private void writeDefaultNode(DefaultNode node) throws IOException {
        final var properties = node.properties();
        if (!properties.isEmpty()) {
            writer.write(Graph.NODE);
            writer.write(' ');
            writeBracketProperties(properties);
            writer.write(';');
            writer.write('\n');
        }
    }

    private void writeEdge(Edge edge) throws IOException {
        writer.write(escape(edge.getNodeFrom().getName()));
        writer.write(" -> ");
        writer.write(escape(edge.getNodeTo().getName()));
        Map<String, String> properties = edge.properties();
        if (!properties.isEmpty()) {
            writer.write(' ');
            writeBracketProperties(properties);
        }
        writer.write(';');
        writer.write('\n');
    }

    public void writeGraph(Graph graph) throws IOException {

        writer.write(MessageFormatter
                .format(Messages.getString("GraphWriter.Output.Header"), DATE_FORMAT.format(new Date())).getMessage());

        writer.write("digraph cluster_vcat{\n");

        // Charset
        writer.write("charset=\"UTF-8\";\n");

        writeLineProperties(graph.properties());

        writeDefaultNode(graph.getDefaultNode());
        writeDefaultEdge(graph.getDefaultEdge());

        for (Group group : graph.getGroups()) {
            writeGroup(group);
        }

        for (Node node : graph.getNodes()) {
            final Set<Edge> edges = graph.getEdgesFrom(node);
            // Write the node; if there are no edges, write it even if it has no properties
            writeNode(node, edges.isEmpty());
            for (Edge edge : edges) {
                writeEdge(edge);
            }
        }

        writer.write('}');
        writer.write('\n');

        writer.close();
    }

    private void writeGroup(Group group) throws IOException {
        writer.write("{\n");
        writeLineProperties(group.properties());
        writeDefaultNode(group.getDefaultNode());
        writeDefaultEdge(group.getDefaultEdge());
        writer.write(group.getNodes().stream()
                .map(Node::getName)
                .map(GraphWriter::escape)
                .collect(Collectors.joining(" "))
        );
        writer.write('\n');
        writer.write('}');
        writer.write('\n');
    }

    private void writeLineProperties(SortedMap<String, String> properties) throws IOException {
        if (!properties.isEmpty()) {
            for (Entry<String, String> property : properties.entrySet()) {
                writeLineProperty(property.getKey(), property.getValue());
            }
        }
    }

    private void writeLineProperty(String key, @Nullable String value) throws IOException {
        writer.write(key);
        writer.write('=');
        if (value != null) {
            writer.write(escape(value));
        }
        writer.write(';');
        writer.write('\n');
    }

    private void writeNode(Node node, boolean writeWithoutProperties) throws IOException {
        final var properties = node.properties();
        // Check if the "label" property is the same as the name; if it is, delete it
        if (node.getName().equals(properties.get("label"))) {
            properties.remove("label");
        }
        // Write node line only if now there are properties, unless overriden by parameter
        if (writeWithoutProperties || !properties.isEmpty()) {
            writer.write(escape(node.getName()));
            if (!properties.isEmpty()) {
                writer.write(' ');
                writeBracketProperties(properties);
            }
            writer.write(';');
            writer.write('\n');
        }
    }
}
