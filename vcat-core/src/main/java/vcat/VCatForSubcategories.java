package vcat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import vcat.graph.Graph;
import vcat.graph.GroupRank;
import vcat.graph.Node;
import vcat.mediawiki.ApiException;
import vcat.mediawiki.ICategoryProvider;
import vcat.mediawiki.IWiki;
import vcat.params.AbstractAllParams;
import vcat.util.AbstractLinkProvider;

public class VCatForSubcategories<W extends IWiki> extends AbstractVCat<W> {

	public VCatForSubcategories(final AbstractAllParams<W> all, final ICategoryProvider<W> categoryProvider,
			final AbstractLinkProvider linkProvider) throws VCatException {
		super(all, categoryProvider, linkProvider);
	}

	protected void renderGraphInnerLoop(Graph graph, Node rootNode, Set<Node> allNodesFound, Collection<Node> newNodes,
			Collection<String> categoryFullTitles, int categoryNamespacePrefixLength) {
		for (String categoryFullTitle : categoryFullTitles) {
			String categoryTitle = categoryFullTitle.substring(categoryNamespacePrefixLength);
			Node categoryNode = graph.node(categoryTitle);
			this.linkProvider.addLinkToNode(categoryNode, categoryFullTitle);
			graph.edge(rootNode, categoryNode);
			if (!allNodesFound.contains(categoryNode)) {
				newNodes.add(categoryNode);
				allNodesFound.add(categoryNode);
			}
		}
	}

	@Override
	protected void renderGraphOuterFirstLoop(Graph graph, Collection<Node> newNodes, Node rootNode,
			Set<Node> allNodesFound, String fullTitle, int categoryNamespacePrefixLength, boolean showhidden)
			throws ApiException {
		Collection<String> categoryFullTitles = this.categoryProvider.requestCategorymembers(this.all.getWiki(),
				fullTitle);
		if (categoryFullTitles != null) {
			renderGraphInnerLoop(graph, rootNode, allNodesFound, newNodes, categoryFullTitles,
					categoryNamespacePrefixLength);
		}
	}

	@Override
	protected void renderGraphOuterLoop(Graph graph, Collection<Node> newNodes, Collection<Node> curNodes,
			Set<Node> allNodesFound, String categoryNamespacePrefix, int categoryNamespacePrefixLength,
			boolean showhidden, boolean exceed) throws ApiException {

		// Create a list of the full titles (including namespace) of the categories in the current loop iteration
		ArrayList<String> curFullTitles = new ArrayList<String>(curNodes.size());
		for (Node curNode : curNodes) {
			curFullTitles.add(categoryNamespacePrefix + curNode.getName());
		}

		for (String fullTitle : curFullTitles) {
			// Request subcategories using API
			List<String> categoryFullTitles = this.categoryProvider.requestCategorymembers(this.all.getWiki(),
					fullTitle);
			// For each API result, first get the node it contains categories for
			String baseTitle = fullTitle.substring(categoryNamespacePrefixLength);
			Node baseNode = graph.node(baseTitle);
			// Then get the list of these categories
			if (exceed) {
				// If the depth limit has been reached, normal processing is replaced by this
				if (!categoryFullTitles.isEmpty()) {
					int unlinkedEdgesRemaining = categoryFullTitles.size();
					// Now, just one special case remains - it is possible we already have nodes in the graph that
					// should have edges with the baseNode. So we look for these and only connect those.
					for (String categoryFullTitle : categoryFullTitles) {
						// Remove "Category:" prefix
						String categoryTitle = categoryFullTitle.substring(categoryNamespacePrefixLength);
						// Add edge to graph if the graph already contains a node
						if (graph.containsNode(categoryTitle)) {
							Node categoryNode = graph.node(categoryTitle);
							this.linkProvider.addLinkToNode(categoryNode, categoryFullTitle);
							graph.edge(baseNode, categoryNode);
							unlinkedEdgesRemaining--;
						}
					}
					// If we have not covered all edges with this, there is an unknown subtree hidden.
					// The node needs a "..." node to show the graph is incomplete.
					if (unlinkedEdgesRemaining > 0) {
						Node exceedNode = graph.node(baseNode.getName() + NODE_EXCEED_SUFFIX);
						exceedNode.setLabel(NODE_EXCEED_LABEL);
						// graph.edge(exceedNode, baseNode);
						graph.edge(baseNode, exceedNode);
						// Keep these excess nodes in the list of new nodes. This is OK because this is the last loop
						// iteration.
						newNodes.add(exceedNode);
					}
				}
			} else {
				// Normal processing - loop through all categories
				for (String categoryFullTitle : categoryFullTitles) {
					// Remove "Category:" prefix
					String categoryTitle = categoryFullTitle.substring(categoryNamespacePrefixLength);
					// Add node to graph
					Node categoryNode = graph.node(categoryTitle);
					this.linkProvider.addLinkToNode(categoryNode, categoryFullTitle);
					graph.edge(baseNode, categoryNode);
					// If we had not encountered node before (will happen with loops!) we record it as a new node and
					// remember we have already seen it
					if (!allNodesFound.contains(categoryNode)) {
						newNodes.add(categoryNode);
						allNodesFound.add(categoryNode);
					}
				}
			}
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
