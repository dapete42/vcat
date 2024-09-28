package org.toolforge.vcat;

import org.toolforge.vcat.graph.Graph;
import org.toolforge.vcat.graph.GroupRank;
import org.toolforge.vcat.graph.Node;
import org.toolforge.vcat.mediawiki.ApiException;
import org.toolforge.vcat.mediawiki.interfaces.CategoryProvider;
import org.toolforge.vcat.params.AbstractAllParams;

import java.util.Collection;
import java.util.Set;

public class VCatForSubcategories extends AbstractVCat {

    public VCatForSubcategories(final AbstractAllParams all, final CategoryProvider categoryProvider) {
        super(all, categoryProvider);
    }

    protected void renderGraphInnerLoop(
            Graph graph, Node rootNode, Set<Node> allNodesFound, Collection<Node> newNodes,
            Collection<String> categoryFullTitles, String categoryNamespacePrefix) {
        for (String categoryFullTitle : categoryFullTitles) {
            String categoryTitle = categoryFullTitle.substring(categoryNamespacePrefix.length());
            Node categoryNode = graph.node(categoryTitle);
            all.getVCat().getLinkProvider().addLinkToNode(categoryNode, categoryFullTitle);
            graph.edge(rootNode, categoryNode);
            if (!allNodesFound.contains(categoryNode)) {
                newNodes.add(categoryNode);
                allNodesFound.add(categoryNode);
            }
        }
    }

    @Override
    protected void renderGraphOuterFirstLoop(
            Graph graph, Collection<Node> newNodes, Node rootNode, Set<Node> allNodesFound, String fullTitle,
            String categoryNamespacePrefix, boolean showHidden) throws ApiException {
        final Collection<String> categoryFullTitles = categoryProvider.requestCategorymembers(all.getWiki(), fullTitle);
        if (!categoryFullTitles.isEmpty()) {
            renderGraphInnerLoop(graph, rootNode, allNodesFound, newNodes, categoryFullTitles, categoryNamespacePrefix);
        }
    }

    @Override
    protected void renderGraphOuterLoop(
            Graph graph, Collection<Node> newNodes, Collection<Node> curNodes, Set<Node> allNodesFound,
            String categoryNamespacePrefix, boolean showHidden, boolean exceedDepth) throws ApiException {

        final CreateEdgeFunction createEdgeFunction = graph::edgeReverse;

        for (var curNode : curNodes) {
            final var categoryFullTitles = categoryProvider
                    .requestCategorymembers(all.getWiki(), categoryNamespacePrefix + curNode.getName());
            final String baseTitle = curNode.getName();
            renderGraphInnerLoop(graph, newNodes, allNodesFound, categoryNamespacePrefix, exceedDepth, baseTitle,
                    categoryFullTitles, createEdgeFunction);
        }
    }

    @Override
    protected GroupRank renderGraphExceedRank() {
        return GroupRank.Max;
    }

    @Override
    protected GroupRank renderGraphRootRank() {
        return GroupRank.Min;
    }

}
