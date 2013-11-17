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
import vcat.graphviz.Graphviz;
import vcat.graphviz.GraphvizException;
import vcat.mediawiki.ApiException;
import vcat.mediawiki.ICategoryProvider;
import vcat.mediawiki.IWiki;
import vcat.mediawiki.Metadata;
import vcat.params.AbstractAllParams;
import vcat.params.TitleNamespaceParam;
import vcat.params.VCatParams;
import vcat.params.Relation;

public abstract class AbstractVCat<W extends IWiki> {

	class Root extends TitleNamespaceParam {

		private static final long serialVersionUID = -1206610398197602542L;

		private final String fullTitle;

		private final Node node;

		public Root(Node node, String title, int namespace, String fullTitle) {
			super(title, namespace);
			this.fullTitle = fullTitle;
			this.node = node;
		}

		public String getFullTitle() {
			return this.fullTitle;
		}

		public Node getNode() {
			return this.node;
		}

	}

	private final static String GRAPH_FONT = "DejaVu Sans";

	private final static String GROUP_EXCEED = "exceed";

	private final static String GROUP_ROOT = "rootGroup";

	private final static String LABEL_DEPTH_PREFIX = "d:";

	private final static String LABEL_HIDDEN = "hidden";

	private final static String LABEL_REL_PREFIX = "rel=";

	protected final static String NODE_EXCEED_LABEL = "…";

	protected final static String NODE_EXCEED_SUFFIX = "_more";

	private final static String ROOT_NODE_PREFIX = "ROOT";

	private final static String TEMPFILE_PREFIX = "graph-temp-";

	private final static String TEMPFILE_SUFFIX = ".gv";

	private final Log log = LogFactory.getLog(this.getClass());

	protected final AbstractAllParams<W> all;

	protected final ICategoryProvider<W> categoryProvider;

	protected AbstractVCat(final AbstractAllParams<W> all, final ICategoryProvider<W> categoryProvider) {
		this.all = all;
		this.categoryProvider = categoryProvider;
	}

	public AbstractAllParams<W> getAllParams() {
		return this.all;
	}

	protected String getDefaultGraphLabel(Collection<Root> roots) {
		final StringBuilder sb = new StringBuilder(this.all.getVCat().getWiki().getDisplayName());
		for (Root root : roots) {
			sb.append(' ').append(root.getFullTitle());
		}
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

		final String categoryNamespacePrefix = metadata.getAuthoritativeName(Metadata.NamespaceCategory) + ':';
		final int categoryNamespacePrefixLength = categoryNamespacePrefix.length();

		String fullTitle;

		try {
			HashSet<Node> allNodesFound = new HashSet<Node>();

			Collection<TitleNamespaceParam> titleNamespaceList = vCatParams.getTitleNamespaceParams();
			ArrayList<Root> roots = new ArrayList<Root>(titleNamespaceList.size());

			int n = 0;
			for (TitleNamespaceParam titleNamespace : titleNamespaceList) {
				final String title = titleNamespace.getTitle();
				final int namespace = titleNamespace.getNamespace();

				fullTitle = this.all.getMetadata().fullTitle(title, namespace);

				Node rootNode;
				if (namespace == Metadata.NamespaceCategory) {
					// For categories, use the title without namespace as the name
					rootNode = graph.node(title);
				} else {
					// Otherwise use "ROOT" with a number and set a label with the full title
					rootNode = graph.node(ROOT_NODE_PREFIX + n);
					n++;
					rootNode.setLabel(fullTitle);
				}

				roots.add(new Root(rootNode, title, namespace, fullTitle));
				allNodesFound.add(rootNode);
			}

			boolean showhidden = vCatParams.isShowhidden();

			ArrayList<Node> newNodes = new ArrayList<Node>();

			for (Root root : roots) {
				this.renderGraphOuterFirstLoop(graph, newNodes, root.getNode(), allNodesFound, root.getFullTitle(),
						categoryNamespacePrefixLength, showhidden);
			}

			// Counting depth and storing various information to be used when it is exceeded
			int curDepth = 0;
			boolean exceedDepth = false;
			Integer limit = vCatParams.getLimit();

			while (!newNodes.isEmpty() && !exceedDepth) {
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
			}

			final StringBuilder graphLabel = new StringBuilder(this.getDefaultGraphLabel(roots));

			Relation relation = this.all.getVCat().getRelation();
			if (relation != Relation.Category) {
				graphLabel.append(' ');
				graphLabel.append(LABEL_REL_PREFIX);
				graphLabel.append(relation.name());
			}

			if (showhidden) {
				graphLabel.append(' ');
				graphLabel.append(LABEL_HIDDEN);
			}

			if (exceedDepth && !newNodes.isEmpty()) {
				// We have gone below the depth. If there are actually any excess nodes, change graph title and group
				// these nodes.
				graphLabel.append(' ');
				graphLabel.append(LABEL_DEPTH_PREFIX);
				graphLabel.append(maxDepth);

				Group exceedGroup = graph.group(GROUP_EXCEED);
				for (Node node : newNodes) {
					node.setStyle(Graphviz.STYLE_DASHED);
					exceedGroup.addNode(node);
				}
				exceedGroup.setRank(this.renderGraphExceedRank());
			}

			renderGraphDefaultFormatting(graphLabel.toString(), graph, roots);

		} catch (ApiException e) {
			throw new VCatException("Error creating graph", e);
		}

		long endMillis = System.currentTimeMillis();

		log.info(String.format("Created category graph with %d nodes. Total run time: %d ms.", graph.getNodeCount(),
				endMillis - startMillis));

		return graph;

	}

	protected void renderGraphDefaultFormatting(String graphLabel, Graph graph, Collection<Root> roots) {
		graph.setFontname(GRAPH_FONT);
		graph.setFontsize(12);
		graph.setLabel(graphLabel);
		graph.setSplines(true);

		graph.getDefaultNode().setFontname(GRAPH_FONT);
		graph.getDefaultNode().setFontsize(12);
		graph.getDefaultNode().setShape(Graphviz.SHAPE_RECT);

		Group groupMin = graph.group(GROUP_ROOT);
		groupMin.setRank(this.renderGraphRootRank());
		for (Root root : roots) {
			Node rootNode = root.getNode();
			rootNode.setLabel(root.getTitle());
			rootNode.setStyle(Graphviz.STYLE_BOLD);
			groupMin.addNode(rootNode);
		}
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

		// Prepare a temporary file
		File tmpFile;
		try {
			tmpFile = File.createTempFile(TEMPFILE_PREFIX, TEMPFILE_SUFFIX);
		} catch (IOException e) {
			throw new VCatException("Error creating temporary file for graph", e);
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
