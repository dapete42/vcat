package vcat;

import java.io.IOException;
import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vcat.graph.Graph;
import vcat.graph.Group;
import vcat.graph.GroupRank;
import vcat.graph.Node;
import vcat.graphviz.GraphWriter;
import vcat.graphviz.GraphvizException;
import vcat.mediawiki.ApiException;
import vcat.mediawiki.interfaces.CategoryProvider;
import vcat.mediawiki.interfaces.Wiki;
import vcat.mediawiki.Metadata;
import vcat.params.AbstractAllParams;
import vcat.params.Relation;
import vcat.params.TitleNamespaceParam;
import vcat.params.VCatParams;

public abstract class AbstractVCat<W extends Wiki> {

	static class Root extends TitleNamespaceParam {

		@Serial
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

	/** Log4j2 Logger */
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

	protected final AbstractAllParams<W> all;

	protected final CategoryProvider<W> categoryProvider;

	protected AbstractVCat(final AbstractAllParams<W> all, final CategoryProvider<W> categoryProvider) {
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

		final String categoryNamespacePrefix = metadata.getAuthoritativeName(Metadata.NS_CATEGORY) + ':';
		final int categoryNamespacePrefixLength = categoryNamespacePrefix.length();

		String fullTitle;

		try {
			HashSet<Node> allNodesFound = new HashSet<>();

			Collection<TitleNamespaceParam> titleNamespaceList = vCatParams.getTitleNamespaceParams();
			ArrayList<Root> roots = new ArrayList<>(titleNamespaceList.size());

			int n = 0;
			for (TitleNamespaceParam titleNamespace : titleNamespaceList) {
				final String title = titleNamespace.getTitle();
				final int namespace = titleNamespace.getNamespace();

				fullTitle = this.all.getMetadata().fullTitle(title, namespace);

				Node rootNode;
				if (namespace == Metadata.NS_CATEGORY) {
					// For categories, use the title without namespace as the name
					rootNode = graph.node(title);
				} else {
					// Otherwise use "ROOT" with a number and set a label with the full title
					rootNode = graph.node(ROOT_NODE_PREFIX + n);
					n++;
					rootNode.setLabel(fullTitle);
				}
				all.getVCat().getLinkProvider().addLinkToNode(rootNode, fullTitle);

				roots.add(new Root(rootNode, title, namespace, fullTitle));
				allNodesFound.add(rootNode);
			}

			boolean showhidden = vCatParams.isShowhidden();

			ArrayList<Node> newNodes = new ArrayList<>();

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
				newNodes = new ArrayList<>();

				this.renderGraphOuterLoop(graph, newNodes, curNodes, allNodesFound, categoryNamespacePrefix,
						categoryNamespacePrefixLength, showhidden, exceedDepth);

				if (limit != null && graph.getNodeCount() > limit && curDepth > 1) {
					// If curDepth and maxDepth end up the same, reduce curDepth by one
					if (maxDepth != null && curDepth == maxDepth) {
						curDepth--;
					}
					return renderGraphForDepth(curDepth);
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
					node.setStyle(Graph.STYLE_DASHED);
					exceedGroup.addNode(node);
				}
				exceedGroup.setRank(this.renderGraphExceedRank());
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
		groupMin.setRank(this.renderGraphRootRank());
		for (Root root : roots) {
			Node rootNode = root.getNode();
			rootNode.setLabel(root.getTitle());
			rootNode.setStyle(Graph.STYLE_BOLD);
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

	public void renderToFile(Path outputFile) throws VCatException, GraphvizException {
		Graph graph = this.renderGraph();
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
