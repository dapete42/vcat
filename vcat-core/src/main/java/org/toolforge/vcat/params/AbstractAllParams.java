package org.toolforge.vcat.params;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.helpers.MessageFormatter;
import org.toolforge.vcat.Messages;
import org.toolforge.vcat.VCatException;
import org.toolforge.vcat.mediawiki.ApiException;
import org.toolforge.vcat.mediawiki.Metadata;
import org.toolforge.vcat.mediawiki.interfaces.MetadataProvider;
import org.toolforge.vcat.mediawiki.interfaces.Wiki;
import org.toolforge.vcat.util.AbstractLinkProvider;

import java.util.*;

/**
 * A bundle of {@link CombinedParams} plus temporary objects needed to build the category graph. Can only be constructed
 * from a set of request parameters, such as these passed in a ServletRequest; this is parsed, and parameters etc. are
 * created as necessary.
 *
 * @author Peter Schlömer
 */
public abstract class AbstractAllParams {

    private static final int MAX_LIMIT = 250;

    public static final String PARAM_ALGORITHM = "algorithm";

    public static final String PARAM_CATEGORY = "category";

    public static final String PARAM_DEPTH = "depth";

    public static final String PARAM_FORMAT = "format";

    public static final String PARAM_LIMIT = "limit";

    public static final String PARAM_LINKS = "links";

    public static final String PARAM_NAMESPACE = "ns";

    public static final String PARAM_RELATION = "rel";

    public static final String PARAM_SHOWHIDDEN = "showhidden";

    public static final String PARAM_TITLE = "title";

    public static final String PARAM_WIKI = "wiki";

    private final CombinedParams combinedParams = new CombinedParams();

    @Getter
    @Nullable
    protected Metadata metadata;

    @Getter
    @Nullable
    private String renderUrl;

    protected final MultivaluedMap<String, String> requestParams = new MultivaluedHashMap<>();

    protected void init(MultivaluedMap<String, String> requestParams, String renderUrl, MetadataProvider metadataProvider) throws VCatException {

        // Remember the URL used to render a graph
        this.renderUrl = renderUrl;

        // Get a copy of the parameters to keep
        this.requestParams.clear();
        this.requestParams.putAll(requestParams);

        // Get a copy of the parameters we can modify
        final MultivaluedMap<String, String> params = new MultivaluedHashMap<>(requestParams);

        String wikiString = getAndRemove(params, PARAM_WIKI);
        if (wikiString == null || wikiString.isEmpty()) {
            throw new VCatException(Messages.getString("AbstractAllParams.Exception.WikiMissing"));
        }
        Wiki wiki = initWiki(wikiString);
        this.getVCat().setWiki(wiki);

        try {
            metadata = metadataProvider.requestMetadata(wiki);
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
                throw new VCatException(
                        MessageFormatter.format(Messages.getString("AbstractAllParams.Exception.UnknownValue"),
                                PARAM_FORMAT, formatString).getMessage());
            }
        }

        Algorithm algorithm = Algorithm.DOT;
        if (algorithmString != null && !algorithmString.isEmpty()) {
            algorithm = Algorithm.valueOfIgnoreCase(algorithmString);
            if (algorithm == null) {
                throw new VCatException(
                        MessageFormatter.format(Messages.getString("AbstractAllParams.Exception.UnknownValue"),
                                PARAM_ALGORITHM, algorithmString).getMessage());
            }
        }

        this.getGraphviz().setOutputFormat(format);
        this.getGraphviz().setAlgorithm(algorithm);

        //
        // Parameter for vCat creation
        //

        var categoryStrings = getAndRemoveMulti(params, PARAM_CATEGORY);
        var titleStrings = getAndRemoveMulti(params, PARAM_TITLE);
        var namespaceString = getAndRemove(params, PARAM_NAMESPACE);
        var depthString = getAndRemove(params, PARAM_DEPTH);
        var limitString = getAndRemove(params, PARAM_LIMIT);
        var showhiddenString = getAndRemove(params, PARAM_SHOWHIDDEN);
        var relationString = getAndRemove(params, PARAM_RELATION);
        var linksString = getAndRemove(params, PARAM_LINKS);

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
        for (String titleString : titleStrings) {
            String[] split = titleString.split("\\|");
            for (String s : split) {
                final String title = unescapeMediawikiTitle(s);
                if (title == null || title.isEmpty()) {
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
                throw new VCatException(
                        MessageFormatter.format(Messages.getString("AbstractAllParams.Exception.InvalidNumber"),
                                PARAM_NAMESPACE, namespaceString).getMessage(),
                        e);
            }
            if (this.metadata.getAllNames(namespace).isEmpty()) {
                throw new VCatException(MessageFormatter
                        .format(Messages.getString("AbstractAllParams.Exception.NamespaceDoesNotExist"), namespace)
                        .getMessage());
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
                throw new VCatException(
                        MessageFormatter.format(Messages.getString("AbstractAllParams.Exception.InvalidNumber"),
                                PARAM_DEPTH, depthString).getMessage(),
                        e);
            }
            if (depth < 1) {
                throw new VCatException(MessageFormatter
                        .format(Messages.getString("AbstractAllParams.Exception.MustBeGreaterThanOrEqual"), PARAM_DEPTH,
                                1)
                        .getMessage());
            }
        }

        // 'limit' - default is MAX_LIMIT, unless outputting raw graphviz test, and cannot be set higher
        int maxLimit = (format == OutputFormat.GraphvizRaw) ? Integer.MAX_VALUE : MAX_LIMIT;
        int limit = MAX_LIMIT;
        if (limitString != null) {
            try {
                limit = Integer.parseInt(limitString);
            } catch (NumberFormatException e) {
                throw new VCatException(
                        MessageFormatter.format(Messages.getString("AbstractAllParams.Exception.InvalidNumber"),
                                PARAM_LIMIT, limitString).getMessage(),
                        e);
            }
            if (limit < 1) {
                throw new VCatException(MessageFormatter
                        .format(Messages.getString("AbstractAllParams.Exception.MustBeGreaterThanOrEqual"), PARAM_LIMIT,
                                1)
                        .getMessage());
            } else if (limit > maxLimit) {
                throw new VCatException(
                        MessageFormatter.format(Messages.getString("AbstractAllParams.Exception.MustBeLessThanOrEqual"),
                                PARAM_LIMIT, maxLimit).getMessage());
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
                case Category -> {
                    // all OK
                }
                case Subcategory -> {
                    for (TitleNamespaceParam titleNamespace : titleNamespaceList) {
                        if (titleNamespace.getNamespace() != Metadata.NS_CATEGORY) {
                            throw new VCatException(MessageFormatter
                                    .format(Messages.getString("AbstractAllParams.Exception.RelOnlyForCategories"),
                                            relationString)
                                    .getMessage());
                        }
                    }
                }
                case null, default -> throw new VCatException(
                        MessageFormatter.format(Messages.getString("AbstractAllParams.Exception.UnknownValue"),
                                PARAM_RELATION, relationString).getMessage());
            }
        }

        // 'links' - whether to include links, and if yes, where they lead
        Links links = Links.None;
        if (linksString != null) {
            links = Links.valueOfIgnoreCase(linksString);
            if (links == null) {
                throw new VCatException(
                        MessageFormatter.format(Messages.getString("AbstractAllParams.Exception.UnknownValue"),
                                PARAM_LINKS, linksString).getMessage());
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
            throw new VCatException(
                    MessageFormatter.format(Messages.getString("AbstractAllParams.Exception.UnknownParameters"),
                            '\'' + StringUtils.join(params.keySet(), "', '") + '\'').getMessage());
        }

    }

    @Nullable
    protected static String getAndRemove(MultivaluedMap<String, String> params, String key) throws VCatException {
        var values = params.get(key);
        if (values == null) {
            return null;
        } else if (values.isEmpty()) {
            params.remove(key);
            return "";
        } else if (values.size() == 1) {
            params.remove(key);
            return values.getFirst();
        } else {
            throw new VCatException(MessageFormatter
                    .format(Messages.getString("AbstractAllParams.Exception.ParameterRepeated"), key).getMessage());
        }
    }

    @Nullable
    protected static List<String> getAndRemoveMulti(MultivaluedMap<String, String> params, String key) {
        var values = params.get(key);
        if (values == null) {
            return null;
        } else {
            params.remove(key);
            return values;
        }
    }

    /**
     * Determine Wiki from supplied wiki parameter.
     *
     * @param wikiString Value of wiki parameter.
     * @return Wiki corresponding to wikiString.
     * @throws VCatException If the wiki object could not be created.
     */
    protected abstract Wiki initWiki(String wikiString) throws VCatException;

    @Nullable
    private static String unescapeMediawikiTitle(@Nullable String title) {
        if (title == null) {
            return null;
        } else {
            return title.replace('_', ' ');
        }
    }

    public CombinedParams getCombined() {
        return combinedParams;
    }

    public GraphvizParams getGraphviz() {
        return combinedParams.getGraphviz();
    }

    public Map<String, List<String>> getRequestParams() {
        return Collections.unmodifiableMap(requestParams);
    }

    public VCatParams getVCat() {
        return combinedParams.getVCat();
    }

    public Wiki getWiki() {
        return combinedParams.getVCat().getWiki();
    }

}
