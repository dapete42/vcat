package vcat.params;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import vcat.VCatException;
import vcat.mediawiki.ApiException;
import vcat.mediawiki.IMetadataProvider;
import vcat.mediawiki.IWiki;
import vcat.mediawiki.Metadata;

/**
 * A bundle of {@link CombinedParams} plus temporary objects needed to build the category graph. Can only be constructed
 * from a set of request parameters, such as these passed in a ServletRequest; this is parsed, and parameters etc. are
 * created as necessary.
 * 
 * @author Peter Schlömer
 */
public abstract class AbstractAllParams<W extends IWiki> {

	private static final int MAX_LIMIT = 500;

	private final CombinedParams<W> combinedParams = new CombinedParams<W>();

	private Metadata metadata;

	protected void init(final Map<String, String[]> requestParams, final IMetadataProvider metadataProvider)
			throws VCatException {

		// Get a copy of the parameters we can modify
		Map<String, String> singleParams = new HashMap<String, String>();
		Map<String, String[]> multiParams = new HashMap<String, String[]>();
		for (Entry<String, String[]> entry : requestParams.entrySet()) {
			String key = entry.getKey();
			String[] values = entry.getValue();
			if (values.length == 1) {
				singleParams.put(key, values[0]);
			} else {
				multiParams.put(key, values);
			}
		}

		// Currently, multiple parameters are not supported at all - if any are set, this is an error
		if (!multiParams.isEmpty()) {
			throw new VCatException("Parameter(s) supplied twice: '" + StringUtils.join(multiParams.keySet(), "', '")
					+ "'.");
		}

		String wikiString = getAndRemove(singleParams, "wiki");
		if (wikiString == null) {
			throw new VCatException("Parameter 'wiki' missing.");
		}
		W wiki = initWiki(wikiString);
		this.getVCat().setWiki(wiki);

		try {
			this.metadata = metadataProvider.requestMetadata(wiki);
		} catch (ApiException e) {
			throw new VCatException("Error retrieving metadata", e);
		}

		//
		// Parameters for graphviz itself
		//

		String formatString = getAndRemove(singleParams, "format");
		String algorithmString = getAndRemove(singleParams, "algorithm");

		OutputFormat format = OutputFormat.PNG;
		if (formatString != null) {
			if ("dot".equals(formatString)) {
				format = OutputFormat.GraphvizRaw;
			} else {
				format = OutputFormat.valueOfIgnoreCase(formatString);
			}
			if (format == null) {
				throw new VCatException("Unknown value for parameter 'format': '" + formatString + "'");
			}
		}

		Algorithm algorithm = Algorithm.DOT;
		if (algorithmString != null) {
			algorithm = Algorithm.valueOfIgnoreCase(algorithmString);
			if (algorithm == null) {
				throw new VCatException("Unknown value for parameter 'algorithm': '" + algorithmString + "'");
			}
		}

		this.getGraphviz().setOutputFormat(format);
		this.getGraphviz().setAlgorithm(algorithm);

		//
		// Parameter for vCat creation
		//

		String category = getAndRemove(singleParams, "category");
		String title = getAndRemove(singleParams, "title");
		String ns = getAndRemove(singleParams, "ns");
		String depthString = getAndRemove(singleParams, "depth");
		String limitString = getAndRemove(singleParams, "limit");
		String showhiddenString = getAndRemove(singleParams, "showhidden");
		String relationString = getAndRemove(singleParams, "rel");

		// 'category'
		if (category != null) {
			// If set, convert it to 'title' and 'ns' parameters; the other parameter may not also be used
			if (title != null || ns != null) {
				throw new VCatException("Parameters 'title' and 'ns' may not be used together with 'category'.");
			}
			title = category;
			ns = Integer.toString(Metadata.NS_CATEGORY);
		}

		// 'title'
		if (title == null) {
			throw new VCatException("Parameter 'title' or 'category' missing.");
		}
		title = unescapeMediawikiTitle(title);

		// 'ns' (namespace)
		int namespace = 0;
		if (ns == null) {
			// Automatically determine namespace from title. Checks if it starts with a namespace; if not, the default
			// (0) is used.
			namespace = this.metadata.namespaceFromTitle(title);
			title = this.metadata.titleWithoutNamespace(title);
		} else {
			// Try to parse 'ns' as an integer
			try {
				namespace = Integer.parseInt(ns);
			} catch (NumberFormatException e) {
				throw new VCatException("Parameter 'ns': '" + ns + "' is not a valid number.", e);
			}
			if (this.metadata.getAllNames(namespace).isEmpty()) {
				throw new VCatException("Parameter 'ns': namespace " + namespace + " does not exist.");
			}
		}

		// 'depth'
		Integer depth = null;
		if (depthString != null) {
			try {
				depth = Integer.parseInt(depthString);
			} catch (NumberFormatException e) {
				throw new VCatException("Parameter 'depth': '" + depthString + "' is not a valid number.", e);
			}
			if (depth < 1) {
				throw new VCatException("Parameter 'depth' must be greator than or equal to 1.");
			}
		}

		// 'limit' - default is MAX_LIMIT, unless outputting raw graphviz test, and cannot be set higher
		int maxLimit = (format == OutputFormat.GraphvizRaw) ? Integer.MAX_VALUE : MAX_LIMIT;
		Integer limit = MAX_LIMIT;
		if (limitString != null) {
			try {
				limit = Integer.parseInt(limitString);
			} catch (NumberFormatException e) {
				throw new VCatException("Parameter 'limit': '" + limitString + "' is not a valid number.", e);
			}
			if (limit < 1) {
				throw new VCatException("Parameter 'limit' must be greater than or equal to 1.");
			} else if (limit > maxLimit) {
				throw new VCatException("Parameter 'depth' must be less than or equal to " + maxLimit + ".");
			}
		}

		// 'showhidden'
		boolean showhidden = (showhiddenString != null);

		// 'rel' - relation to use (categories, subcategories)
		Relation relation = Relation.Category;
		if (relationString != null) {
			relation = Relation.valueOfIgnoreCase(relationString);
			switch (relation) {
			case Category:
				// all ok
				break;
			case Subcategory:
				if (namespace != Metadata.NS_CATEGORY) {
					throw new VCatException("Parameter 'rel=" + relationString + "' can only be used for categories");
				}
				break;
			default:
				throw new VCatException("Unknown value for parameter 'rel': '" + relationString + "'");
			}
		}

		// Move values to parameters
		this.getVCat().setTitle(title);
		this.getVCat().setNamespace(namespace);
		this.getVCat().setDepth(depth);
		this.getVCat().setLimit(limit);
		this.getVCat().setShowhidden(showhidden);
		this.getVCat().setRelation(relation);

		//
		// After all this handling, no parameters should be left
		//

		if (!singleParams.isEmpty()) {
			throw new VCatException("Unknown parameter(s): '" + StringUtils.join(singleParams.keySet(), "', '") + "'");
		}

	}

	protected static String getAndRemove(Map<String, String> params, String key) {
		String value = params.get(key);
		params.remove(key);
		return value;
	}

	/**
	 * Determine Wiki from supplied wiki parameter.
	 * 
	 * @param wikiString
	 * @return
	 * @throws VCatException
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

	public VCatParams<W> getVCat() {
		return this.combinedParams.getVCat();
	}

	public W getWiki() {
		return this.combinedParams.getVCat().getWiki();
	}

}
