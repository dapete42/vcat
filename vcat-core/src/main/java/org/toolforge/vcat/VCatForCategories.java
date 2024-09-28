package org.toolforge.vcat;

import org.toolforge.vcat.graph.Graph;
import org.toolforge.vcat.graph.GroupRank;
import org.toolforge.vcat.graph.Node;
import org.toolforge.vcat.mediawiki.ApiException;
import org.toolforge.vcat.mediawiki.interfaces.CategoryProvider;
import org.toolforge.vcat.params.AbstractAllParams;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class VCatForCategories extends AbstractVCat {

    public VCatForCategories(final AbstractAllParams all, final CategoryProvider categoryProvider) {
        super(all, categoryProvider);
    }

    @Override
    protected void renderGraphOuterFirstLoop(Graph graph, Collection<Node> newNodes, Node rootNode, Set<Node> allNodesFound, String fullTitle,
                                             String categoryNamespacePrefix, boolean showHidden) throws ApiException {
        List<String> rootFullTitles = Collections.singletonList(fullTitle);
        {
            Collection<String> categoryFullTitles = categoryProvider
                    .requestCategories(all.getWiki(), rootFullTitles, showHidden).get(fullTitle);
            if (categoryFullTitles != null) {
                renderGraphInnerFirstLoop(graph, rootNode, allNodesFound, newNodes, categoryFullTitles,
                        categoryNamespacePrefix);
            }
        }
    }

    private void renderGraphInnerFirstLoop(Graph graph, Node rootNode, Set<Node> allNodesFound, Collection<Node> newNodes,
                                           Collection<String> categoryFullTitles, String categoryNamespacePrefix) {
        for (String categoryFullTitle : categoryFullTitles) {
            final String categoryTitle = categoryFullTitle.substring(categoryNamespacePrefix.length());
            final var categoryNode = graph.node(categoryTitle);
            all.getVCat().getLinkProvider().addLinkToNode(categoryNode, categoryFullTitle);
            graph.edge(categoryNode, rootNode);
            if (!allNodesFound.contains(categoryNode)) {
                newNodes.add(categoryNode);
                allNodesFound.add(categoryNode);
            }
        }
    }

    @Override
    protected void renderGraphOuterLoop(
            Graph graph, Collection<Node> newNodes, Collection<Node> curNodes, Set<Node> allNodesFound,
            String categoryNamespacePrefix, boolean showHidden, boolean exceedDepth) throws ApiException {

        // Create a list of the full titles (including namespace) needed for API call
        var curFullTitles = curNodes.stream()
                .map(Node::getName)
                .map(name -> categoryNamespacePrefix + name)
                .toList();

        final CreateEdgeFunction createEdgeFunction = graph::edge;

        // Look at categories from API
        for (var categoryFullEntry : categoryProvider.requestCategories(all.getWiki(), curFullTitles, showHidden)
                .entrySet()) {
            final String baseTitle = categoryFullEntry.getKey().substring(categoryNamespacePrefix.length());
            final var categoryFullTitles = categoryFullEntry.getValue();
            renderGraphInnerLoop(graph, newNodes, allNodesFound, categoryNamespacePrefix, exceedDepth, baseTitle,
                    categoryFullTitles, createEdgeFunction);
        }

    }

    @Override
    protected GroupRank renderGraphExceedRank() {
        return GroupRank.Min;
    }

    @Override
    protected GroupRank renderGraphRootRank() {
        return GroupRank.Max;
    }

}
