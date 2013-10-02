package vcat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import vcat.cache.CacheException;
import vcat.cache.file.GraphFileCache;
import vcat.graph.Graph;
import vcat.graph.Group;
import vcat.graph.GroupRank;
import vcat.graph.Node;
import vcat.graphviz.GraphWriter;
import vcat.graphviz.GraphvizException;
import vcat.mediawiki.ApiException;
import vcat.mediawiki.ICategoryProvider;
import vcat.mediawiki.IWiki;
import vcat.mediawiki.Metadata;
import vcat.params.AbstractAllParams;
import vcat.params.VCatParams;
import vcat.params.Relation;

public abstract class AbstractVCat<W extends IWiki> {

	private Log log = LogFactory.getLog(this.getClass());

	protected final AbstractAllParams<W> all;

	protected final ICategoryProvider<W> categoryProvider;

	protected AbstractVCat(final AbstractAllParams<W> all, final ICategoryProvider<W> categoryProvider) {
		this.all = all;
		this.categoryProvider = categoryProvider;
	}

	public AbstractAllParams<W> getAllParams() {
		return this.all;
	}

	protected String getDefaultGraphLabel(String fullTitle) {
		final StringBuilder sb = new StringBuilder(this.all.getVCat().getWiki().getDisplayName());
		sb.append(' ').append(fullTitle);
		return sb.toString();
	}

	private Graph renderGraph() throws VCatException {
		return this.renderGraphForDepth(this.all.getVCat().getDepth());
	}

	private Graph renderGraphForDepth(Integer maxDepth) throws VCatException {

		final long startMillis = System.currentTimeMillis();

		final Graph graph = new Graph();

		final Metadata metadata = this.all.getMetadata();
		final VCatParams<W> vCatParams = this.all.getVCat();

		final String categoryNamespacePrefix = metadata.getAuthoritativeName(Metadata.NS_CATEGORY) + ':';
		final int categoryNamespacePrefixLength = categoryNamespacePrefix.length();

		String fullTitle;

		try {

			fullTitle = this.all.getMetadata().fullTitle(vCatParams.getTitle(), vCatParams.getNamespace());

			Node rootNode;
			if (this.all.getVCat().getNamespace() == Metadata.NS_CATEGORY) {
				// For categories, use the title without namespace as the name
				rootNode = graph.node(vCatParams.getTitle());
			} else {
				// Otherwise use "ROOT" and set a label with the full title
				rootNode = graph.node("ROOT");
				rootNode.setLabel(fullTitle);
			}

			boolean showhidden = all.getVCat().isShowhidden();

			HashSet<Node> allNodesFound = new HashSet<Node>();
			allNodesFound.add(rootNode);

			ArrayList<Node> newNodes = new ArrayList<Node>();

			this.renderGraphOuterFirstLoop(graph, newNodes, rootNode, allNodesFound, fullTitle,
					categoryNamespacePrefixLength, showhidden);

			// Counting depth and storing various information to be used when it is exceeded
			int curDepth = 0;
			boolean exceedDepth = false;
			Integer limit = vCatParams.getLimit();

			while (!newNodes.isEmpty()) {
				curDepth++;

				if (maxDepth != null && curDepth >= maxDepth) {
					exceedDepth = true;
				}

				// The new nodes from the last loop iteration are now the current ones. Move them and prepare a new list
				// of new nodes.
				Collection<Node> curNodes = newNodes;
				newNodes = new ArrayList<Node>();

				this.renderGraphOuterLoop(graph, newNodes, curNodes, allNodesFound, categoryNamespacePrefix,
						categoryNamespacePrefixLength, showhidden, exceedDepth);

				if (limit != null && graph.getNodeCount() > limit && curDepth > 1) {
					return renderGraphForDepth(curDepth - 1);
				}

				if (exceedDepth) {
					break;
				}
			}

			final StringBuilder graphLabel = new StringBuilder(this.getDefaultGraphLabel(fullTitle));

			Relation relation = this.all.getVCat().getRelation();
			if (relation != Relation.Category) {
				graphLabel.append(" rel=");
				graphLabel.append(relation.name());
			}

			if (exceedDepth && !newNodes.isEmpty()) {
				// We have gone below the depth. If there are actually any excess nodes, change graph title and group
				// these nodes.
				graphLabel.append(" d:");
				graphLabel.append(maxDepth);

				Group exceedGroup = graph.group("exceed");
				for (Node node : newNodes) {
					node.setStyle("dashed");
					exceedGroup.addNode(node);
				}
				exceedGroup.setRank(this.renderGraphExceedRank());
			}

			renderGraphDefaultFormatting(graphLabel.toString(), graph, rootNode);

		} catch (ApiException e) {
			throw new VCatException("Error creating graph", e);
		}

		long endMillis = System.currentTimeMillis();

		log.info("Created category graph with " + graph.getNodeCount() + " nodes for '" + fullTitle
				+ "'. Total run time: " + (endMillis - startMillis) + " ms.");

		return graph;

	}

	protected void renderGraphDefaultFormatting(String graphLabel, Graph graph, Node rootNode) {
		graph.setFontname("DejaVu Sans");
		graph.setFontsize(12);
		graph.setLabel(graphLabel);
		graph.setSplines(true);

		graph.getDefaultNode().setFontname("DejaVu Sans");
		graph.getDefaultNode().setFontsize(12);
		graph.getDefaultNode().setShape("rect");

		rootNode.setLabel(this.all.getVCat().getTitle());
		rootNode.setStyle("bold");

		Group groupMin = graph.group("rootGroup");
		groupMin.addNode(rootNode);
		groupMin.setRank(this.renderGraphRootRank());
	}

	protected abstract GroupRank renderGraphExceedRank();

	protected abstract void renderGraphOuterFirstLoop(Graph graph, Collection<Node> newNodes, Node rootNode,
			Set<Node> allNodesFound, String fullTitle, int categoryNamespacePrefixLength, boolean showhidden)
			throws ApiException;

	protected abstract void renderGraphOuterLoop(Graph graph, Collection<Node> newNodes, Collection<Node> curNodes,
			Set<Node> allNodesFound, String categoryNamespacePrefix, int categoryNamespacePrefixLength,
			boolean showhidden, boolean exceed) throws ApiException;

	protected abstract GroupRank renderGraphRootRank();

	public void renderToCache(GraphFileCache<W> cache) throws CacheException, VCatException, GraphvizException {
		VCatParams<W> vCatParams = this.all.getVCat();

		// Check if already in cache; nothing to do in that case
		if (cache.containsKey(vCatParams)) {
			return;
		}

		// Prepara a temporary file
		File tmpFile;
		try {
			tmpFile = File.createTempFile("Graph-temp-", ".gv");
		} catch (IOException e) {
			throw new VCatException("Failed to create temporary file", e);
		}

		Graph graph = this.renderGraph();

		try {
			GraphWriter.writeGraphToFile(graph, tmpFile);
		} catch (GraphvizException e) {
			tmpFile.delete();
			throw e;
		}

		try {
			cache.putFile(vCatParams, tmpFile, true);
		} catch (CacheException e) {
			tmpFile.delete();
			throw e;
		}
	}

}
