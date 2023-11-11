package vcat;

import vcat.graph.Edge;
import vcat.graph.Graph;
import vcat.graph.GroupRank;
import vcat.graph.Node;
import vcat.mediawiki.ApiException;
import vcat.mediawiki.interfaces.CategoryProvider;
import vcat.mediawiki.interfaces.Wiki;
import vcat.params.AbstractAllParams;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

public class VCatForSubcategories<W extends Wiki> extends AbstractVCat<W> {

    public VCatForSubcategories(final AbstractAllParams<W> all, final CategoryProvider<W> categoryProvider) {
        super(all, categoryProvider);
    }

    protected void renderGraphInnerLoop(Graph graph, Node rootNode, Set<Node> allNodesFound, Collection<Node> newNodes,
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
    protected void renderGraphOuterFirstLoop(Graph graph, Collection<Node> newNodes, Node rootNode,
                                             Set<Node> allNodesFound, String fullTitle, String categoryNamespacePrefix,
                                             boolean showHidden)
            throws ApiException {
        Collection<String> categoryFullTitles = categoryProvider.requestCategorymembers(all.getWiki(), fullTitle);
        if (categoryFullTitles != null) {
            renderGraphInnerLoop(graph, rootNode, allNodesFound, newNodes, categoryFullTitles, categoryNamespacePrefix);
        }
    }

    @Override
    protected void renderGraphOuterLoop(Graph graph, Collection<Node> newNodes, Collection<Node> curNodes,
                                        Set<Node> allNodesFound, String categoryNamespacePrefix,
                                        boolean showhidden, boolean exceedDepth) throws ApiException {

        // Create a list of the full titles (including namespace) of the categories in the current loop iteration
        var curFullTitles = curNodes.stream()
                .map(Node::getName)
                .map(name -> categoryNamespacePrefix + name)
                .toList();

        final BiFunction<Node, Node, Edge> createEdgeFunction = graph::edgeReverse;

        for (String fullTitle : curFullTitles) {
            final var categoryFullTitles = categoryProvider.requestCategorymembers(all.getWiki(), fullTitle);
            // For each API result, first get the node it contains categories for
            final String baseTitle = fullTitle.substring(categoryNamespacePrefix.length());
            renderGraphInnerLoop(graph, newNodes, allNodesFound, categoryNamespacePrefix, exceedDepth, baseTitle,
                    categoryFullTitles, createEdgeFunction);
        }
    }

    @Override
    protected GroupRank renderGraphExceedRank() {
        return GroupRank.max;
    }

    @Override
    protected GroupRank renderGraphRootRank() {
        return GroupRank.min;
    }

}
