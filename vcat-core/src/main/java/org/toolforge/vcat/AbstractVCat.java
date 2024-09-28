package org.toolforge.vcat;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.toolforge.vcat.graph.*;
import org.toolforge.vcat.graphviz.GraphWriter;
import org.toolforge.vcat.graphviz.GraphvizException;
import org.toolforge.vcat.mediawiki.ApiException;
import org.toolforge.vcat.mediawiki.Metadata;
import org.toolforge.vcat.mediawiki.interfaces.CategoryProvider;
import org.toolforge.vcat.params.AbstractAllParams;
import org.toolforge.vcat.params.Relation;
import org.toolforge.vcat.params.TitleNamespaceParam;

import java.io.IOException;
import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;

public abstract class AbstractVCat {

    @FunctionalInterface
    protected interface CreateEdgeFunction extends BiFunction<Node, Node, Edge> {
    }

    @Getter
    protected static class Root extends TitleNamespaceParam {

        @Serial
        private static final long serialVersionUID = 8946252039152427686L;

        private final String fullTitle;

        private final Node node;

        public Root(Node node, String title, int namespace, String fullTitle) {
            super(title, namespace);
            this.fullTitle = fullTitle;
            this.node = node;
        }

    }

    /**
     * Log4j2 Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AbstractVCat.class);

    private static final String GRAPH_FONT = "DejaVu Sans";

    private static final String GROUP_EXCEED = "exceed";

    private static final String GROUP_ROOT = "rootGroup";

    private static final String LABEL_DEPTH_PREFIX = "d:";

    private static final String LABEL_HIDDEN = "hidden";

    private static final String LABEL_REL_PREFIX = "rel=";

    protected static final String NODE_EXCEED_LABEL = "…";

    protected static final String NODE_EXCEED_SUFFIX = "_more";

    private static final String ROOT_NODE_PREFIX = "ROOT";

    protected final AbstractAllParams all;

    protected final CategoryProvider categoryProvider;

    protected AbstractVCat(final AbstractAllParams all, final CategoryProvider categoryProvider) {
        this.all = all;
        this.categoryProvider = categoryProvider;
    }

    protected String getDefaultGraphLabel(Collection<Root> roots) {
        final StringBuilder sb = new StringBuilder(all.getVCat().getWiki().getDisplayName());
        for (Root root : roots) {
            sb.append(' ').append(root.getFullTitle());
        }
        return sb.toString();
    }

    private Graph renderGraph() throws VCatException {
        return renderGraphForDepth(all.getVCat().getDepth());
    }

    private Graph renderGraphForDepth(@Nullable Integer maxDepth) throws VCatException {

        final long startMillis = System.currentTimeMillis();

        final var graph = new Graph();

        final String categoryNamespacePrefix = all.getMetadata().getAuthoritativeName(Metadata.NS_CATEGORY) + ':';

        try {
            final HashSet<Node> allNodesFound = new HashSet<>();

            final var titleNamespaceList = all.getVCat().getTitleNamespaceParams();
            final ArrayList<Root> roots = new ArrayList<>(titleNamespaceList.size());

            int namespaceCounter = 0;
            for (TitleNamespaceParam titleNamespace : titleNamespaceList) {
                final String title = titleNamespace.getTitle();
                final int namespace = titleNamespace.getNamespace();

                final String fullTitle = all.getMetadata().fullTitle(title, namespace);

                final Node rootNode;
                if (namespace == Metadata.NS_CATEGORY) {
                    // For categories, use the title without namespace as the name
                    rootNode = graph.node(title);
                } else {
                    // Otherwise use "ROOT" with a number and set a label with the full title
                    rootNode = graph.node(ROOT_NODE_PREFIX + namespaceCounter);
                    namespaceCounter++;
                    rootNode.setLabel(fullTitle);
                }
                all.getVCat().getLinkProvider().addLinkToNode(rootNode, fullTitle);

                roots.add(new Root(rootNode, title, namespace, fullTitle));
                allNodesFound.add(rootNode);
            }

            ArrayList<Node> newNodes = new ArrayList<>();

            for (Root root : roots) {
                renderGraphOuterFirstLoop(graph, newNodes, root.getNode(), allNodesFound, root.getFullTitle(),
                        categoryNamespacePrefix, all.getVCat().isShowhidden());
            }

            // Counting depth and storing various information to be used when it is exceeded
            int curDepth = 0;
            boolean exceedDepth = false;
            final Integer limit = all.getVCat().getLimit();

            while (!newNodes.isEmpty() && !exceedDepth) {
                curDepth++;

                if (maxDepth != null && curDepth >= maxDepth) {
                    exceedDepth = true;
                }

                // The new nodes from the last loop iteration are now the current ones. Move them and prepare a new list
                // of new nodes.
                Collection<Node> curNodes = newNodes;
                newNodes = new ArrayList<>();

                renderGraphOuterLoop(graph, newNodes, curNodes, allNodesFound, categoryNamespacePrefix,
                        all.getVCat().isShowhidden(), exceedDepth);

                if (limit != null && graph.getNodeCount() > limit && curDepth > 1) {
                    // If curDepth and maxDepth end up the same, reduce curDepth by one
                    if (maxDepth != null && curDepth == maxDepth) {
                        curDepth--;
                    }
                    return renderGraphForDepth(curDepth);
                }
            }

            final StringBuilder graphLabel = new StringBuilder(getDefaultGraphLabel(roots));

            Relation relation = all.getVCat().getRelation();
            if (relation != Relation.Category) {
                graphLabel.append(' ')
                        .append(LABEL_REL_PREFIX)
                        .append(relation.name());
            }

            if (all.getVCat().isShowhidden()) {
                graphLabel.append(' ')
                        .append(LABEL_HIDDEN);
            }

            if (exceedDepth && !newNodes.isEmpty()) {
                // We have gone below the depth. If there are actually any excess nodes, change graph title and group
                // these nodes.
                graphLabel.append(' ')
                        .append(LABEL_DEPTH_PREFIX)
                        .append(maxDepth);

                final var exceedGroup = graph.group(GROUP_EXCEED);
                for (var node : newNodes) {
                    node.setStyle(Graph.STYLE_DASHED);
                    exceedGroup.addNode(node);
                }
                exceedGroup.setRank(renderGraphExceedRank());
            }

            renderGraphDefaultFormatting(graphLabel.toString(), graph, roots);

        } catch (ApiException e) {
            throw new VCatException(Messages.getString("AbstractVCat.Exception.CreatingGraph"), e);
        }

        long endMillis = System.currentTimeMillis();

        LOG.info(Messages.getString("AbstractVCat.Info.CreatedGraph"), graph.getNodeCount(),
                endMillis - startMillis);

        return graph;

    }

    protected void renderGraphDefaultFormatting(String graphLabel, Graph graph, Collection<Root> roots) {
        graph.setFontname(GRAPH_FONT);
        graph.setFontsize(12);
        graph.setLabel(graphLabel);
        graph.setSplines(true);

        graph.getDefaultNode().setFontname(GRAPH_FONT);
        graph.getDefaultNode().setFontsize(12);
        graph.getDefaultNode().setShape(Graph.SHAPE_RECT);

        Group groupMin = graph.group(GROUP_ROOT);
        groupMin.setRank(renderGraphRootRank());
        for (Root root : roots) {
            Node rootNode = root.getNode();
            rootNode.setLabel(root.getTitle());
            rootNode.setStyle(Graph.STYLE_BOLD);
            groupMin.addNode(rootNode);
        }
    }

    protected abstract GroupRank renderGraphExceedRank();

    protected abstract void renderGraphOuterFirstLoop(Graph graph, Collection<Node> newNodes, Node rootNode, Set<Node> allNodesFound, String fullTitle,
                                                      String categoryNamespacePrefix, boolean showHidden) throws ApiException;

    protected abstract void renderGraphOuterLoop(Graph graph, Collection<Node> newNodes, Collection<Node> curNodes, Set<Node> allNodesFound,
                                                 String categoryNamespacePrefix, boolean showHidden, boolean exceedDepth) throws ApiException;

    protected final void renderGraphInnerLoop(
            Graph graph, Collection<Node> newNodes, Set<Node> allNodesFound, String categoryNamespacePrefix,
            boolean exceedDepth, String baseTitle, Collection<String> categoryFullTitles,
            CreateEdgeFunction createEdgeFunction) {
        final var baseNode = graph.node(baseTitle);
        if (exceedDepth) {
            // If the depth limit has been reached, normal processing is replaced by this
            if (!categoryFullTitles.isEmpty()) {
                int unlinkedEdgesRemaining = categoryFullTitles.size();
                // Now, just one special case remains - it is possible we already have nodes in the graph that
                // should have edges with the baseNode. So we look for these and only connect those.
                for (String categoryFullTitle : categoryFullTitles) {
                    // Remove "Category:" prefix
                    final String categoryTitle = categoryFullTitle.substring(categoryNamespacePrefix.length());
                    // Add edge to graph if the graph already contains a node
                    if (graph.containsNode(categoryTitle)) {
                        final var categoryNode = graph.node(categoryTitle);
                        all.getVCat().getLinkProvider().addLinkToNode(categoryNode, categoryFullTitle);
                        createEdgeFunction.apply(categoryNode, baseNode);
                        unlinkedEdgesRemaining--;
                    }
                }
                // If we have not covered all edges with this, there is an unknown subtree hidden.
                // The node needs a "..." node to show the graph is incomplete.
                if (unlinkedEdgesRemaining > 0) {
                    final var exceedNode = graph.node(baseNode.getName() + NODE_EXCEED_SUFFIX);
                    exceedNode.setLabel(NODE_EXCEED_LABEL);
                    createEdgeFunction.apply(exceedNode, baseNode);
                    // Keep these excess nodes in the list of new nodes. This is OK because this is the last loop
                    // iteration.
                    newNodes.add(exceedNode);
                }
            }
        } else {
            // Normal processing - loop through all categories
            for (String categoryFullTitle : categoryFullTitles) {
                // Remove "Category:" prefix
                final String categoryTitle = categoryFullTitle.substring(categoryNamespacePrefix.length());
                // Add node to graph
                final var categoryNode = graph.node(categoryTitle);
                all.getVCat().getLinkProvider().addLinkToNode(categoryNode, categoryFullTitle);
                createEdgeFunction.apply(categoryNode, baseNode);
                // If we had not encountered node before (will happen with loops!) we record it as a new node and
                // remember we have already seen it
                if (!allNodesFound.contains(categoryNode)) {
                    newNodes.add(categoryNode);
                    allNodesFound.add(categoryNode);
                }
            }
        }
    }

    protected abstract GroupRank renderGraphRootRank();

    public void renderToFile(Path outputFile) throws VCatException, GraphvizException {
        Graph graph = renderGraph();
        try {
            GraphWriter.writeGraphToFile(graph, outputFile);
        } catch (GraphvizException e) {
            try {
                Files.delete(outputFile);
            } catch (IOException ee) {
                e.addSuppressed(ee);
            }
            throw e;
        }
    }

}
