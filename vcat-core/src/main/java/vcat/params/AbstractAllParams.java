package vcat.params;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableMap;

import vcat.Messages;
import vcat.VCatException;
import vcat.mediawiki.ApiException;
import vcat.mediawiki.IMetadataProvider;
import vcat.mediawiki.IWiki;
import vcat.mediawiki.Metadata;
import vcat.util.AbstractLinkProvider;

/**
 * A bundle of {@link CombinedParams} plus temporary objects needed to build the category graph. Can only be constructed
 * from a set of request parameters, such as these passed in a ServletRequest; this is parsed, and parameters etc. are
 * created as necessary.
 * 
 * @author Peter Schlömer
 */
public abstract class AbstractAllParams<W extends IWiki> {

	private static final int MAX_LIMIT = 250;

	private static final String PARAM_ALGORITHM = "algorithm";

	private static final String PARAM_CATEGORY = "category";

	private static final String PARAM_DEPTH = "depth";

	private static final String PARAM_FORMAT = "format";

	private static final String PARAM_LIMIT = "limit";

	private static final String PARAM_LINKS = "links";

	private static final String PARAM_NAMESPACE = "ns";

	private static final String PARAM_RELATION = "rel";

	private static final String PARAM_SHOWHIDDEN = "showhidden";

	private static final String PARAM_TITLE = "title";

	private static final String PARAM_WIKI = "wiki";

	private final CombinedParams<W> combinedParams = new CombinedParams<>();

	private Metadata metadata;

	private String renderUrl;

	private final Map<String, String[]> requestParams = new HashMap<>();

	protected void init(final Map<String, String[]> requestParams, final String renderUrl,
			final IMetadataProvider metadataProvider) throws VCatException {

		// Remember the URL used to render a graph
		this.renderUrl = renderUrl;

		// Get a copy of the parameters to keep
		this.requestParams.clear();
		this.requestParams.putAll(requestParams);

		// Get a copy of the parameters we can modify
		final HashMap<String, String[]> params = new HashMap<>(requestParams);

		String wikiString = getAndRemove(params, PARAM_WIKI);
		if (wikiString == null || wikiString.isEmpty()) {
			throw new VCatException(Messages.getString("AbstractAllParams.Exception.WikiMissing"));
		}
		W wiki = initWiki(wikiString);
		this.getVCat().setWiki(wiki);

		try {
			this.metadata = metadataProvider.requestMetadata(wiki);
		} catch (ApiException e) {
			throw new VCatException(Messages.getString("AbstractAllParams.Exception.RetrievingMetadata"), e);
		}

		//
		// Parameters for graphviz itself
		//

		String formatString = getAndRemove(params, PARAM_FORMAT);
		String algorithmString = getAndRemove(params, PARAM_ALGORITHM);

		OutputFormat format = OutputFormat.PNG;
		if (formatString != null && !formatString.isEmpty()) {
			format = OutputFormat.valueOfIgnoreCase(formatString);
			if (format == null) {
				throw new VCatException(String.format(Messages.getString("AbstractAllParams.Exception.UnknownValue"),
						PARAM_FORMAT, formatString));
			}
		}

		Algorithm algorithm = Algorithm.DOT;
		if (algorithmString != null && !algorithmString.isEmpty()) {
			algorithm = Algorithm.valueOfIgnoreCase(algorithmString);
			if (algorithm == null) {
				throw new VCatException(String.format(Messages.getString("AbstractAllParams.Exception.UnknownValue"),
						PARAM_ALGORITHM, algorithmString));
			}
		}

		this.getGraphviz().setOutputFormat(format);
		this.getGraphviz().setAlgorithm(algorithm);

		//
		// Parameter for vCat creation
		//

		String[] categoryStrings = getAndRemoveMulti(params, PARAM_CATEGORY);
		String[] titleStrings = getAndRemoveMulti(params, PARAM_TITLE);
		String namespaceString = getAndRemove(params, PARAM_NAMESPACE);
		String depthString = getAndRemove(params, PARAM_DEPTH);
		String limitString = getAndRemove(params, PARAM_LIMIT);
		String showhiddenString = getAndRemove(params, PARAM_SHOWHIDDEN);
		String relationString = getAndRemove(params, PARAM_RELATION);
		String linksString = getAndRemove(params, PARAM_LINKS);

		// 'category'
		if (categoryStrings != null) {
			// If set, convert it to 'title' and 'ns' parameters; the other parameter may not also be used
			if (titleStrings != null || namespaceString != null) {
				throw new VCatException(Messages.getString("AbstractAllParams.Exception.TitleNsWithCategory"));
			}
			titleStrings = categoryStrings;
			namespaceString = Integer.toString(Metadata.NS_CATEGORY);
		}

		// 'title'
		final Set<String> titles = new TreeSet<>();
		if (titleStrings == null) {
			throw new VCatException(Messages.getString("AbstractAllParams.Exception.TitleCategoryMissing"));
		}
		for (int i = 0; i < titleStrings.length; i++) {
			String[] split = titleStrings[i].split("\\|");
			for (int j = 0; j < split.length; j++) {
				final String title = unescapeMediawikiTitle(split[j]);
				if (title.isEmpty()) {
					throw new VCatException(Messages.getString("AbstractAllParams.Exception.TitleEmpty"));
				}
				titles.add(title);
			}
		}

		// Build TitleNamespaceParams when handling namespaces
		ArrayList<TitleNamespaceParam> titleNamespaceList = new ArrayList<>(titles.size());

		// 'ns' (namespace)
		if (namespaceString == null) {
			// Automatically determine namespace from titles. Checks if it starts with a namespace; if not, the default
			// (0) is used.
			for (String title : titles) {
				titleNamespaceList.add(new TitleNamespaceParam(this.metadata.titleWithoutNamespace(title),
						this.metadata.namespaceFromTitle(title)));
			}
		} else {
			// Try to parse 'ns' as an integer
			int namespace;
			try {
				namespace = Integer.parseInt(namespaceString);
			} catch (NumberFormatException e) {
				throw new VCatException(String.format(Messages.getString("AbstractAllParams.Exception.InvalidNumber"),
						PARAM_NAMESPACE, namespaceString), e);
			}
			if (this.metadata.getAllNames(namespace).isEmpty()) {
				throw new VCatException(String.format(
						Messages.getString("AbstractAllParams.Exception.NamespaceDoesNotExist"), namespace));
			}
			for (String title : titles) {
				titleNamespaceList.add(new TitleNamespaceParam(title, namespace));
			}
		}

		// 'depth'
		Integer depth = null;
		if (depthString != null) {
			try {
				depth = Integer.parseInt(depthString);
			} catch (NumberFormatException e) {
				throw new VCatException(String.format(Messages.getString("AbstractAllParams.Exception.InvalidNumber"),
						PARAM_DEPTH, depthString), e);
			}
			if (depth < 1) {
				throw new VCatException(String.format(
						Messages.getString("AbstractAllParams.Exception.MustBeGreaterThanOrEqual"), PARAM_DEPTH, 1));
			}
		}

		// 'limit' - default is MAX_LIMIT, unless outputting raw graphviz test, and cannot be set higher
		int maxLimit = (format == OutputFormat.GraphvizRaw) ? Integer.MAX_VALUE : MAX_LIMIT;
		Integer limit = MAX_LIMIT;
		if (limitString != null) {
			try {
				limit = Integer.parseInt(limitString);
			} catch (NumberFormatException e) {
				throw new VCatException(String.format(Messages.getString("AbstractAllParams.Exception.InvalidNumber"),
						PARAM_LIMIT, limitString), e);
			}
			if (limit < 1) {
				throw new VCatException(String.format(
						Messages.getString("AbstractAllParams.Exception.MustBeGreaterThanOrEqual"), PARAM_LIMIT, 1));
			} else if (limit > maxLimit) {
				throw new VCatException(String.format(
						Messages.getString("AbstractAllParams.Exception.MustBeLessThanOrEqual"), PARAM_LIMIT, maxLimit));
			}
		}

		// 'showhidden'
		boolean showhidden;
		if (showhiddenString == null || "0".equals(showhiddenString)) {
			showhidden = false;
		} else if ("1".equals(showhiddenString)) {
			showhidden = true;
		} else {
			throw new VCatException(Messages.getString("AbstractAllParams.Exception.ShowhiddenInvalid"));
		}

		// 'rel' - relation to use (categories, subcategories)
		Relation relation = Relation.Category;
		if (relationString != null) {
			relation = Relation.valueOfIgnoreCase(relationString);
			switch (relation) {
			case Category:
				// all ok
				break;
			case Subcategory:
				for (TitleNamespaceParam titleNamespace : titleNamespaceList) {
					if (titleNamespace.getNamespace() != Metadata.NS_CATEGORY) {
						throw new VCatException(String.format(
								Messages.getString("AbstractAllParams.Exception.RelOnlyForCategories"), relationString));
					}
				}
				break;
			default:
				throw new VCatException(String.format(Messages.getString("AbstractAllParams.Exception.UnknownValue"),
						PARAM_RELATION, relationString));
			}
		}

		// 'links' - whether to include links, and if yes, where they lead
		Links links = Links.None;
		if (linksString != null) {
			links = Links.valueOfIgnoreCase(linksString);
			if (links == null) {
				throw new VCatException(String.format(Messages.getString("AbstractAllParams.Exception.UnknownValue"),
						PARAM_LINKS, linksString));
			}
		}

		// Move values to parameters
		this.getVCat().setTitleNamespaceParams(titleNamespaceList);
		this.getVCat().setDepth(depth);
		this.getVCat().setLimit(limit);
		this.getVCat().setShowhidden(showhidden);
		this.getVCat().setRelation(relation);
		this.getVCat().setLinks(links);

		// Create link provider
		this.getVCat().setLinkProvider(AbstractLinkProvider.fromParams(this));

		//
		// After all this handling, no parameters should be left
		//

		if (!params.isEmpty()) {
			throw new VCatException(String.format(Messages.getString("AbstractAllParams.Exception.UnknownParameters"),
					'\'' + StringUtils.join(params.keySet(), "', '") + '\''));
		}

	}

	protected static String getAndRemove(Map<String, String[]> params, String key) throws VCatException {
		String[] values = params.get(key);
		if (values == null) {
			return null;
		} else if (values.length == 0) {
			params.remove(key);
			return "";
		} else if (values.length == 1) {
			params.remove(key);
			return values[0];
		} else {
			throw new VCatException(String.format(Messages.getString("AbstractAllParams.Exception.ParameterRepeated"),
					key));
		}
	}

	protected static String[] getAndRemoveMulti(Map<String, String[]> params, String key) throws VCatException {
		String[] values = params.get(key);
		if (values == null) {
			return null;
		} else if (values.length == 0) {
			params.remove(key);
			return values;
		} else {
			params.remove(key);
			return values;
		}
	}

	/**
	 * Determine Wiki from supplied wiki parameter.
	 * 
	 * @param wikiString
	 *            Value of wiki parameter.
	 * @return Wiki corresponding to wikiString.
	 * @throws VCatException
	 *             If the wiki object could not be created.
	 */
	protected abstract W initWiki(final String wikiString) throws VCatException;

	private static String unescapeMediawikiTitle(String title) {
		if (title == null) {
			return null;
		} else {
			return title.replace('_', ' ');
		}
	}

	public CombinedParams<W> getCombined() {
		return this.combinedParams;
	}

	public GraphvizParams getGraphviz() {
		return this.combinedParams.getGraphviz();
	}

	public Metadata getMetadata() {
		return this.metadata;
	}

	public String getRenderUrl() {
		return this.renderUrl;
	}

	public Map<String, String[]> getRequestParams() {
		return ImmutableMap.copyOf(this.requestParams);
	}

	public VCatParams<W> getVCat() {
		return this.combinedParams.getVCat();
	}

	public W getWiki() {
		return this.combinedParams.getVCat().getWiki();
	}

}
